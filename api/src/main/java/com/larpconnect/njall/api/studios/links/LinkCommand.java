package com.larpconnect.njall.api.studios.links;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link LinkActor}. */
public sealed interface LinkCommand {

  @Immutable
  record CreateLink(
      UUID tenantId,
      String studioIdentifier,
      CreateLinkRequest request,
      ActorRef<LinkActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements LinkCommand {
    public CreateLink(
        UUID tenantId,
        String studioIdentifier,
        CreateLinkRequest request,
        ActorRef<LinkActorResponse> replyTo) {
      this(tenantId, studioIdentifier, request, replyTo, Optional.empty());
    }
  }

  @Immutable
  record GetLink(
      UUID tenantId, String studioIdentifier, UUID linkId, ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}

  @Immutable
  record PatchLink(
      UUID tenantId,
      String studioIdentifier,
      UUID linkId,
      UpdateLinkRequest request,
      Optional<String> updateMask,
      ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}

  @Immutable
  record DeleteLink(UUID tenantId, UUID linkId, ActorRef<LinkActorResponse> replyTo)
      implements LinkCommand {}
}
