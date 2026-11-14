package com.larpconnect.njall.api.studios.systems;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link GameSystemActor}. */
public sealed interface GameSystemActorResponse {

  /** Successful single game system response. */
  @Immutable
  record Success(GameSystemResponse system) implements GameSystemActorResponse {}

  /** Successful list of game systems response. */
  @Immutable
  record Items(ImmutableList<GameSystemResponse> systems) implements GameSystemActorResponse {}

  /** Successful game system deletion marker. */
  @Immutable
  record Deleted() implements GameSystemActorResponse {}

  /** Error response carrying an HTTP status code and message. */
  @Immutable
  record Failure(int status, String message) implements GameSystemActorResponse {}

  /** Wraps a single game system response. */
  static GameSystemActorResponse success(GameSystemResponse system) {
    return new Success(system);
  }

  /** Wraps a list of game system responses. */
  static GameSystemActorResponse items(ImmutableList<GameSystemResponse> systems) {
    return new Items(systems);
  }

  /** Returns a deletion confirmation response. */
  static GameSystemActorResponse deleted() {
    return new Deleted();
  }

  /** Creates a 404 Not Found failure response. */
  static GameSystemActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  /** Creates a 400 Bad Request failure response. */
  static GameSystemActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  /** Creates a 409 Conflict failure response. */
  static GameSystemActorResponse conflict(String message) {
    return new Failure(409, message);
  }

  /** Creates a 500 Internal Server Error failure response. */
  static GameSystemActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
