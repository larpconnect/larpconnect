package com.larpconnect.njall.api.studios;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling tenanted physical studio locations and subordinate addresses. */
public final class LocationsRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(LocationsRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<LocationCommand> locationActor;
  private final ActorRef<AddressCommand> addressActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  LocationsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<LocationCommand> locationActor,
      ActorRef<AddressCommand> addressActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(
        studioLookupCache,
        locationActor,
        addressActor,
        system,
        objectMapper,
        Duration.ofSeconds(20));
  }

  LocationsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<LocationCommand> locationActor,
      ActorRef<AddressCommand> addressActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.locationActor = locationActor;
    this.addressActor = addressActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/studios"),
        () ->
            pathPrefix(
                PathMatchers.segment(),
                studioIdParam ->
                    pathPrefix(
                        PathMatchers.separateOnSlashes("v1/locations"),
                        () -> handleLocations(studioIdParam))));
  }

  private Route handleLocations(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(this::buildTenantedRoutes)
        .orElseGet(() -> completeNotFound("Studio not found: " + studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route buildTenantedRoutes(StudioLookup lookup) {
    return concat(
        pathEndOrSingleSlash(() -> post(() -> handlePostLocation(lookup))),
        pathPrefix(
            PathMatchers.segment(),
            locationIdParam -> handleLocationBranch(lookup, locationIdParam)));
  }

  private Route handlePostLocation(StudioLookup lookup) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateLocationRequest.class),
        request ->
            onComplete(
                () -> askCreateLocation(lookup.tenantId(), request),
                res -> mapLocationResponse(res, StatusCodes.CREATED)));
  }

  private Route handleLocationBranch(StudioLookup lookup, String locationIdParam) {
    return LocationValidation.tryParseUuid(locationIdParam)
        .map(locationId -> buildLocationSubroutes(lookup.tenantId(), locationId))
        .orElseGet(() -> completeNotFound("Location not found: " + locationIdParam));
  }

  private Route buildLocationSubroutes(UUID tenantId, UUID locationId) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    get(() -> handleGetLocation(tenantId, locationId)),
                    patch(() -> handlePatchLocation(tenantId, locationId)),
                    delete(() -> handleDeleteLocation(tenantId, locationId)))),
        pathPrefix(
            PathMatchers.separateOnSlashes("addresses"),
            () -> buildAddressesSubroutes(tenantId, locationId)));
  }

  private Route handleGetLocation(UUID tenantId, UUID locationId) {
    return onComplete(
        () -> askGetLocation(tenantId, locationId),
        res -> mapLocationResponse(res, StatusCodes.OK));
  }

  private Route handlePatchLocation(UUID tenantId, UUID locationId) {
    return parameterOptional(
        "update_mask",
        mask ->
            entity(
                Jackson.unmarshaller(objectMapper, UpdateLocationRequest.class),
                req ->
                    onComplete(
                        () -> askPatchLocation(tenantId, locationId, req, mask),
                        res -> mapLocationResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteLocation(UUID tenantId, UUID locationId) {
    return onComplete(
        () -> askDeleteLocation(tenantId, locationId),
        res -> mapLocationResponse(res, StatusCodes.NO_CONTENT));
  }

  private Route buildAddressesSubroutes(UUID tenantId, UUID locationId) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    get(() -> handleListAddresses(tenantId, locationId)),
                    post(() -> handlePostAddress(tenantId, locationId)))),
        path(
            PathMatchers.segment(),
            addressIdParam -> handleAddressItem(tenantId, locationId, addressIdParam)));
  }

  private Route handleListAddresses(UUID tenantId, UUID locationId) {
    return onComplete(
        () -> askListAddresses(tenantId, locationId),
        res -> mapAddressResponse(res, StatusCodes.OK));
  }

  private Route handlePostAddress(UUID tenantId, UUID locationId) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateAddressRequest.class),
        request ->
            onComplete(
                () -> askCreateAddress(tenantId, locationId, request),
                res -> mapAddressResponse(res, StatusCodes.CREATED)));
  }

  private Route handleAddressItem(UUID tenantId, UUID locationId, String addressIdParam) {
    return LocationValidation.tryParseUuid(addressIdParam)
        .map(addressId -> buildAddressItemRoutes(tenantId, locationId, addressId))
        .orElseGet(() -> completeNotFound("Address not found: " + addressIdParam));
  }

  private Route buildAddressItemRoutes(UUID tenantId, UUID locationId, UUID addressId) {
    return concat(
        get(
            () ->
                onComplete(
                    () -> askGetAddress(tenantId, locationId, addressId),
                    res -> mapAddressResponse(res, StatusCodes.OK))),
        patch(
            () ->
                parameterOptional(
                    "update_mask",
                    mask ->
                        entity(
                            Jackson.unmarshaller(objectMapper, UpdateAddressRequest.class),
                            req ->
                                onComplete(
                                    () ->
                                        askPatchAddress(tenantId, locationId, addressId, req, mask),
                                    res -> mapAddressResponse(res, StatusCodes.OK))))),
        delete(
            () ->
                onComplete(
                    () -> askDeleteAddress(tenantId, locationId, addressId),
                    res -> mapAddressResponse(res, StatusCodes.NO_CONTENT))));
  }

  private CompletionStage<LocationActorResponse> askCreateLocation(
      UUID tenantId, CreateLocationRequest request) {
    return ask(
        locationActor,
        replyTo -> new LocationCommand.CreateLocation(tenantId, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<LocationActorResponse> askGetLocation(UUID tenantId, UUID locationId) {
    return ask(
        locationActor,
        replyTo -> new LocationCommand.GetLocation(tenantId, locationId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<LocationActorResponse> askPatchLocation(
      UUID tenantId, UUID locationId, UpdateLocationRequest request, Optional<String> updateMask) {
    return ask(
        locationActor,
        replyTo ->
            new LocationCommand.PatchLocation(tenantId, locationId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<LocationActorResponse> askDeleteLocation(UUID tenantId, UUID locationId) {
    return ask(
        locationActor,
        replyTo -> new LocationCommand.DeleteLocation(tenantId, locationId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AddressActorResponse> askCreateAddress(
      UUID tenantId, UUID locationId, CreateAddressRequest request) {
    return ask(
        addressActor,
        replyTo -> new AddressCommand.CreateAddress(tenantId, locationId, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AddressActorResponse> askGetAddress(
      UUID tenantId, UUID locationId, UUID addressId) {
    return ask(
        addressActor,
        replyTo -> new AddressCommand.GetAddress(tenantId, locationId, addressId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AddressActorResponse> askListAddresses(UUID tenantId, UUID locationId) {
    return ask(
        addressActor,
        replyTo -> new AddressCommand.ListAddresses(tenantId, locationId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AddressActorResponse> askPatchAddress(
      UUID tenantId,
      UUID locationId,
      UUID addressId,
      UpdateAddressRequest request,
      Optional<String> updateMask) {
    return ask(
        addressActor,
        replyTo ->
            new AddressCommand.PatchAddress(
                tenantId, locationId, addressId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AddressActorResponse> askDeleteAddress(
      UUID tenantId, UUID locationId, UUID addressId) {
    return ask(
        addressActor,
        replyTo -> new AddressCommand.DeleteAddress(tenantId, locationId, addressId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapLocationResponse(
      Try<LocationActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Location", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case LocationActorResponse.Success s ->
          complete(successStatus, s.location(), Jackson.marshaller(objectMapper));
      case LocationActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case LocationActorResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              createErrorResponse(400, br.message()),
              Jackson.marshaller(objectMapper));
      case LocationActorResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              createErrorResponse(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case LocationActorResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              createErrorResponse(500, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route mapAddressResponse(
      Try<AddressActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Address", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case AddressActorResponse.Success s ->
          complete(successStatus, s.address(), Jackson.marshaller(objectMapper));
      case AddressActorResponse.ListSuccess ls ->
          complete(StatusCodes.OK, ls.addresses(), Jackson.marshaller(objectMapper));
      case AddressActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case AddressActorResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              createErrorResponse(400, br.message()),
              Jackson.marshaller(objectMapper));
      case AddressActorResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              createErrorResponse(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case AddressActorResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              createErrorResponse(500, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route completeNotFound(String message) {
    return complete(
        StatusCodes.NOT_FOUND, createErrorResponse(404, message), Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(String entityType, Throwable error) {
    logger.error("{} actor request failed", entityType, error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(500, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(int status, String message) {
    return new StudioErrorResponse(status, message);
  }
}
