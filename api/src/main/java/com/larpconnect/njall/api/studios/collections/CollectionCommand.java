package com.larpconnect.njall.api.studios.collections;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link CollectionActor}. */
public sealed interface CollectionCommand {

  enum ItemAction {
    ADD,
    REMOVE
  }

  @Immutable
  record CreateCollection(
      UUID tenantId, CreateCollectionRequest request, ActorRef<CollectionActorResponse> replyTo)
      implements CollectionCommand {}

  @Immutable
  record GetCollection(
      UUID tenantId,
      UUID collectionId,
      Optional<String> form,
      ActorRef<CollectionActorResponse> replyTo)
      implements CollectionCommand {}

  @Immutable
  record PatchCollection(
      UUID tenantId,
      UUID collectionId,
      UpdateCollectionRequest request,
      Optional<String> updateMask,
      ActorRef<CollectionActorResponse> replyTo)
      implements CollectionCommand {}

  @Immutable
  record DeleteCollection(
      UUID tenantId, UUID collectionId, ActorRef<CollectionActorResponse> replyTo)
      implements CollectionCommand {}

  @Immutable
  record MutateItem(
      UUID tenantId,
      UUID collectionId,
      ItemAction action,
      UUID refersToId,
      Optional<UUID> before,
      Optional<Integer> index,
      ActorRef<CollectionActorResponse> replyTo)
      implements CollectionCommand {}
}
