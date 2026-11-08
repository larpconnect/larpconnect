package com.larpconnect.njall.api.studios.tags;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link TagActor}. */
public sealed interface TagCommand {

  @Immutable
  record CreateTags(
      UUID tenantId,
      String studioIdentifier,
      String canonicalPrefix,
      ImmutableList<CreateTagRequest> requests,
      boolean batch,
      ActorRef<TagActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements TagCommand {
    public CreateTags(
        UUID tenantId,
        String studioIdentifier,
        String canonicalPrefix,
        ImmutableList<CreateTagRequest> requests,
        boolean batch,
        ActorRef<TagActorResponse> replyTo) {
      this(tenantId, studioIdentifier, canonicalPrefix, requests, batch, replyTo, Optional.empty());
    }
  }

  @Immutable
  record QueryTag(
      UUID tenantId,
      String studioIdentifier,
      Optional<String> idOrName,
      ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}

  @Immutable
  record PatchTag(
      UUID tenantId,
      String studioIdentifier,
      String idOrName,
      UpdateTagRequest request,
      Optional<String> updateMask,
      ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}

  @Immutable
  record DeleteTag(UUID tenantId, String idOrName, ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}
}
