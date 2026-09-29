package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link LinkActor}. */
public sealed interface LinkCommand {

  @Immutable
  record CreateLink(UUID tenantId, CreateLinkRequest request, ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}

  @Immutable
  record GetLink(UUID tenantId, UUID linkId, ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}

  @Immutable
  record PatchLink(
      UUID tenantId,
      UUID linkId,
      UpdateLinkRequest request,
      Optional<String> updateMask,
      ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}

  @Immutable
  record DeleteLink(UUID tenantId, UUID linkId, ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}
}
