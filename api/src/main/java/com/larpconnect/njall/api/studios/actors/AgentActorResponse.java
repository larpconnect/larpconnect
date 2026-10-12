package com.larpconnect.njall.api.studios.actors;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.api.studios.collections.OrderedCollectionResponse;
import java.util.Optional;

/** Response protocol emitted by {@link AgentActor}. */
public sealed interface AgentActorResponse {

  @Immutable
  record Success(Optional<Object> payload) implements AgentActorResponse {}

  @Immutable
  record Failure(int status, String message) implements AgentActorResponse {}

  static AgentActorResponse success(ActorResponse actor) {
    return new Success(Optional.of(actor));
  }

  static AgentActorResponse collectionSuccess(OrderedCollectionResponse collection) {
    return new Success(Optional.of(collection));
  }

  static AgentActorResponse noContent() {
    return new Success(Optional.empty());
  }

  static AgentActorResponse created() {
    return new Success(Optional.empty());
  }

  static AgentActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  static AgentActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  static AgentActorResponse conflict(String message) {
    return new Failure(409, message);
  }

  static AgentActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
