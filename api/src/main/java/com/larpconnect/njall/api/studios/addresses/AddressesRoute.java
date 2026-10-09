package com.larpconnect.njall.api.studios.addresses;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
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

/** HTTP route handling subordinate address operations for a physical studio location. */
public final class AddressesRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(AddressesRoute.class);

  private final ActorRef<AddressCommand> addressActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  AddressesRoute(
      ActorRef<AddressCommand> addressActor, ActorSystem<Void> system, ObjectMapper objectMapper) {
    this(addressActor, system, objectMapper, Duration.ofSeconds(20));
  }

  public AddressesRoute(
      ActorRef<AddressCommand> addressActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.addressActor = addressActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  public Route addressesSubroutes(UUID tenantId, UUID locationId) {
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
    return tryParseUuid(addressIdParam)
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
              createErrorResponse(StatusCodes.BAD_REQUEST, br.message()),
              Jackson.marshaller(objectMapper));
      case AddressActorResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              createErrorResponse(StatusCodes.NOT_FOUND, nf.message()),
              Jackson.marshaller(objectMapper));
      case AddressActorResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route completeNotFound(String message) {
    return complete(
        StatusCodes.NOT_FOUND,
        createErrorResponse(StatusCodes.NOT_FOUND, message),
        Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(String entityType, Throwable error) {
    logger.error("{} actor request failed", entityType, error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }

  private static Optional<UUID> tryParseUuid(String raw) {
    try {
      return Optional.of(UUID.fromString(raw));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }
}
