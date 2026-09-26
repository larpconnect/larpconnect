package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link StudioActor}. */
public sealed interface StudioActorResponse {

  @Immutable
  record Success(StudioResponse studio) implements StudioActorResponse {}

  @Immutable
  record NotFound(String message) implements StudioActorResponse {}

  @Immutable
  record Failure(String message) implements StudioActorResponse {}

  static StudioActorResponse success(StudioResponse studio) {
    return new Success(studio);
  }

  static StudioActorResponse notFound(String message) {
    return new NotFound(message);
  }

  static StudioActorResponse failure(String message) {
    return new Failure(message);
  }
}
