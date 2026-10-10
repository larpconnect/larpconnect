package com.larpconnect.njall.api.studios.tags;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link TagActor}. */
public sealed interface TagCommand {

  @Immutable
  record CreateTags(
      UUID tenantId,
      String canonicalPrefix,
      ImmutableList<CreateTagRequest> requests,
      boolean batch,
      ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}

  @Immutable
  record QueryTag(UUID tenantId, Optional<String> idOrName, ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}

  @Immutable
  record PatchTag(
      UUID tenantId,
      String idOrName,
      UpdateTagRequest request,
      Optional<String> updateMask,
      ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}

  @Immutable
  record DeleteTag(UUID tenantId, String idOrName, ActorRef<TagActorResponse> replyTo)
      implements TagCommand {}
}
