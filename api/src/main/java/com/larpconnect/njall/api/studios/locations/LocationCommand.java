package com.larpconnect.njall.api.studios.locations;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link LocationActor}. */
public sealed interface LocationCommand {

  @Immutable
  record CreateLocation(
      UUID tenantId,
      String studioIdentifier,
      CreateLocationRequest request,
      ActorRef<LocationActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements LocationCommand {
    public CreateLocation(
        UUID tenantId,
        String studioIdentifier,
        CreateLocationRequest request,
        ActorRef<LocationActorResponse> replyTo) {
      this(tenantId, studioIdentifier, request, replyTo, Optional.empty());
    }
  }

  @Immutable
  record GetLocation(
      UUID tenantId,
      String studioIdentifier,
      UUID locationId,
      ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}

  @Immutable
  record PatchLocation(
      UUID tenantId,
      String studioIdentifier,
      UUID locationId,
      UpdateLocationRequest request,
      Optional<String> updateMask,
      ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}

  @Immutable
  record DeleteLocation(UUID tenantId, UUID locationId, ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}
}
