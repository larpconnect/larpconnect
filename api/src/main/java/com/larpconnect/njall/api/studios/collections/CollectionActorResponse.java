package com.larpconnect.njall.api.studios.collections;

import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link CollectionActor}. */
public sealed interface CollectionActorResponse {

  @Immutable
  record Success(OrderedCollectionResponse collection) implements CollectionActorResponse {}

  @Immutable
  record NoContent() implements CollectionActorResponse {}

  @Immutable
  record Failure(int status, String message) implements CollectionActorResponse {}

  static CollectionActorResponse success(OrderedCollectionResponse collection) {
    return new Success(collection);
  }

  static CollectionActorResponse noContent() {
    return new NoContent();
  }

  static CollectionActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  static CollectionActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  static CollectionActorResponse conflict(String message) {
    return new Failure(409, message);
  }

  static CollectionActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
