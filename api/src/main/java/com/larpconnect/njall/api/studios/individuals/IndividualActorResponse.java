package com.larpconnect.njall.api.studios.individuals;

import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link IndividualActor}. */
public sealed interface IndividualActorResponse {

  @Immutable
  record Success(IndividualResponse individual) implements IndividualActorResponse {}

  @Immutable
  record Deleted() implements IndividualActorResponse {}

  @Immutable
  record Failure(int status, String message) implements IndividualActorResponse {}

  static IndividualActorResponse success(IndividualResponse individual) {
    return new Success(individual);
  }

  static IndividualActorResponse deleted() {
    return new Deleted();
  }

  static IndividualActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  static IndividualActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  static IndividualActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
