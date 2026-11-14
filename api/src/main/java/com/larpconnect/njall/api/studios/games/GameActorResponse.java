package com.larpconnect.njall.api.studios.games;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link GameActor}. */
public sealed interface GameActorResponse {

  /** Successful single-game response payload. */
  @Immutable
  record Success(GameResponse game) implements GameActorResponse {}

  /** Successful list-of-games response payload. */
  @Immutable
  record Items(ImmutableList<GameResponse> games) implements GameActorResponse {}

  /** Successful soft-deletion acknowledgement. */
  @Immutable
  record Deleted() implements GameActorResponse {}

  /** Failed operation response carrying HTTP status code and message. */
  @Immutable
  record Failure(int status, String message) implements GameActorResponse {}

  /**
   * Builds a success response carrying a single game.
   *
   * @param game The game response payload.
   * @return The wrapped response.
   */
  static GameActorResponse success(GameResponse game) {
    return new Success(game);
  }

  /**
   * Builds a success response carrying a list of games.
   *
   * @param games The game items.
   * @return The wrapped response.
   */
  static GameActorResponse items(ImmutableList<GameResponse> games) {
    return new Items(games);
  }

  /**
   * Builds a deletion acknowledgement response.
   *
   * @return The deleted marker response.
   */
  static GameActorResponse deleted() {
    return new Deleted();
  }

  /**
   * Builds a 404 Not Found failure response.
   *
   * @param message Human-readable error message.
   * @return The 404 failure response.
   */
  static GameActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  /**
   * Builds a 400 Bad Request failure response.
   *
   * @param message Human-readable validation error message.
   * @return The 400 failure response.
   */
  static GameActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  /**
   * Builds a 500 Internal Server Error failure response.
   *
   * @param message Human-readable error message.
   * @return The 500 failure response.
   */
  static GameActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
