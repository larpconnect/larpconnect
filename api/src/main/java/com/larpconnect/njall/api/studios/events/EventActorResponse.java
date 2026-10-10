package com.larpconnect.njall.api.studios.events;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link EventActor}. */
public sealed interface EventActorResponse {

  @Immutable
  record Success(EventResponse event) implements EventActorResponse {}

  @Immutable
  record Items(ImmutableList<EventResponse> events) implements EventActorResponse {}

  @Immutable
  record Deleted() implements EventActorResponse {}

  @Immutable
  record Failure(int status, String message) implements EventActorResponse {}

  static EventActorResponse success(EventResponse event) {
    return new Success(event);
  }

  static EventActorResponse items(ImmutableList<EventResponse> events) {
    return new Items(events);
  }

  static EventActorResponse deleted() {
    return new Deleted();
  }

  static EventActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  static EventActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  static EventActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
