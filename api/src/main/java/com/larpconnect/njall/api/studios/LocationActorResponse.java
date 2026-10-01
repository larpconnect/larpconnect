package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link LocationActor}. */
public sealed interface LocationActorResponse {

  @Immutable
  record Success(LocationResponse location) implements LocationActorResponse {}

  @Immutable
  record Deleted() implements LocationActorResponse {}

  @Immutable
  record NotFound(String message) implements LocationActorResponse {}

  @Immutable
  record BadRequest(String message) implements LocationActorResponse {}

  @Immutable
  record Failure(String message) implements LocationActorResponse {}

  static LocationActorResponse success(LocationResponse location) {
    return new Success(location);
  }

  static LocationActorResponse deleted() {
    return new Deleted();
  }

  static LocationActorResponse notFound(String message) {
    return new NotFound(message);
  }

  static LocationActorResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static LocationActorResponse failure(String message) {
    return new Failure(message);
  }
}
