package com.larpconnect.njall.api.studios.locations;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.api.studios.common.GeneratorUriSupport;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.domain.Location;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted location operations. */
final class LocationActor extends AbstractBehavior<LocationCommand> {

  private final Logger logger = LoggerFactory.getLogger(LocationActor.class);
  private final LocationDAO locationDao;

  LocationActor(ActorContext<LocationCommand> context, LocationDAO locationDao) {
    super(context);
    this.locationDao = locationDao;
  }

  @Override
  public Receive<LocationCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(LocationCommand.CreateLocation.class, this::onCreateLocation)
        .onMessage(LocationCommand.GetLocation.class, this::onGetLocation)
        .onMessage(LocationCommand.PatchLocation.class, this::onPatchLocation)
        .onMessage(LocationCommand.DeleteLocation.class, this::onDeleteLocation)
        .build();
  }

  private Behavior<LocationCommand> onCreateLocation(LocationCommand.CreateLocation cmd) {
    try {
      var validationError = LocationValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(LocationActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      var parsed =
          GeneratorUriSupport.parseGenerator(
              cmd.request().generator(), cmd.studioIdentifier(), cmd.lookup());
      if (parsed.isEmpty()) {
        cmd.replyTo()
            .tell(LocationActorResponse.badRequest("Invalid generator URI or actor identifier"));
        return this;
      }
      executeCreate(
          cmd.tenantId(),
          cmd.studioIdentifier(),
          parsed.orElseThrow(),
          cmd.request(),
          cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create location", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId,
      String studioIdentifier,
      UUID owner,
      CreateLocationRequest request,
      ActorRef<LocationActorResponse> replyTo) {
    var domain = locationDao.create(tenantId, owner, request.name(), request.summary());
    replyTo.tell(LocationActorResponse.success(toLocationResponse(studioIdentifier, domain)));
  }

  private Behavior<LocationCommand> onGetLocation(LocationCommand.GetLocation cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.studioIdentifier(), cmd.locationId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get location", e);
    }
    return this;
  }

  private void executeGet(
      UUID tenantId,
      String studioIdentifier,
      UUID locationId,
      ActorRef<LocationActorResponse> replyTo) {
    locationDao
        .findById(tenantId, locationId)
        .ifPresentOrElse(
            loc ->
                replyTo.tell(
                    LocationActorResponse.success(toLocationResponse(studioIdentifier, loc))),
            () ->
                replyTo.tell(LocationActorResponse.notFound("Location not found: " + locationId)));
  }

  private Behavior<LocationCommand> onPatchLocation(LocationCommand.PatchLocation cmd) {
    try {
      var validationError = LocationValidation.validatePatch(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(LocationActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executePatch(
          cmd.tenantId(),
          cmd.studioIdentifier(),
          cmd.locationId(),
          cmd.request(),
          cmd.updateMask(),
          cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch location", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      String studioIdentifier,
      UUID locationId,
      UpdateLocationRequest request,
      Optional<String> updateMask,
      ActorRef<LocationActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var name = selectField(maskFields, "name", request.name());
    var summary = selectField(maskFields, "summary", request.summary());

    locationDao
        .patch(tenantId, locationId, name, summary)
        .ifPresentOrElse(
            updated ->
                replyTo.tell(
                    LocationActorResponse.success(toLocationResponse(studioIdentifier, updated))),
            () ->
                replyTo.tell(LocationActorResponse.notFound("Location not found: " + locationId)));
  }

  private LocationResponse toLocationResponse(String studioIdentifier, Location location) {
    return new LocationResponse(
        location.id(),
        GeneratorUriSupport.formatGeneratorUri(studioIdentifier, location.generator()),
        location.name(),
        location.summary(),
        location.createdOn(),
        location.updatedOn());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private Optional<String> selectField(
      Optional<Set<String>> maskFields, String fieldName, Optional<String> requestValue) {
    if (maskFields.isPresent()) {
      return maskFields.orElseThrow().contains(fieldName) ? requestValue : Optional.empty();
    }
    return requestValue;
  }

  private Behavior<LocationCommand> onDeleteLocation(LocationCommand.DeleteLocation cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.locationId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete location", e);
    }
    return this;
  }

  private void executeDelete(
      UUID tenantId, UUID locationId, ActorRef<LocationActorResponse> replyTo) {
    var deleted = locationDao.softDelete(tenantId, locationId);
    if (deleted) {
      replyTo.tell(LocationActorResponse.deleted());
    } else {
      replyTo.tell(LocationActorResponse.notFound("Location not found: " + locationId));
    }
  }

  private void handleError(
      ActorRef<LocationActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in LocationActor", operation, error);
    if (error instanceof IllegalArgumentException) {
      replyTo.tell(LocationActorResponse.badRequest(error.getMessage()));
      return;
    }
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(LocationActorResponse.failure(reason));
  }
}
