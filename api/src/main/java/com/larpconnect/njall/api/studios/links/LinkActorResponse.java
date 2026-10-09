package com.larpconnect.njall.api.studios.links;

import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link LinkActor}. */
public sealed interface LinkActorResponse {

  @Immutable
  record Success(LinkResponse link) implements LinkActorResponse {}

  @Immutable
  record Deleted() implements LinkActorResponse {}

  @Immutable
  record NotFound(String message) implements LinkActorResponse {}

  @Immutable
  record BadRequest(String message) implements LinkActorResponse {}

  @Immutable
  record Failure(String message) implements LinkActorResponse {}

  static LinkActorResponse success(LinkResponse link) {
    return new Success(link);
  }

  static LinkActorResponse deleted() {
    return new Deleted();
  }

  static LinkActorResponse notFound(String message) {
    return new NotFound(message);
  }

  static LinkActorResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static LinkActorResponse failure(String message) {
    return new Failure(message);
  }
}
