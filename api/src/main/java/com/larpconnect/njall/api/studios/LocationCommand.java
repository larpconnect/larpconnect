package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link LocationActor}. */
public sealed interface LocationCommand {

  @Immutable
  record CreateLocation(
      UUID tenantId, CreateLocationRequest request, ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}

  @Immutable
  record GetLocation(UUID tenantId, UUID locationId, ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}

  @Immutable
  record PatchLocation(
      UUID tenantId,
      UUID locationId,
      UpdateLocationRequest request,
      Optional<String> updateMask,
      ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}

  @Immutable
  record DeleteLocation(UUID tenantId, UUID locationId, ActorRef<LocationActorResponse> replyTo)
      implements LocationCommand {}
}
