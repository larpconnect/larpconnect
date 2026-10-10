package com.larpconnect.njall.api.studios.tags;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link TagActor}. */
public sealed interface TagActorResponse {

  @Immutable
  record Success(TagResponse tag, boolean created) implements TagActorResponse {}

  @Immutable
  record Items(ImmutableList<TagResponse> tags) implements TagActorResponse {}

  @Immutable
  record Deleted() implements TagActorResponse {}

  @Immutable
  record Failure(int status, String message) implements TagActorResponse {}

  static TagActorResponse success(TagResponse tag, boolean created) {
    return new Success(tag, created);
  }

  static TagActorResponse items(ImmutableList<TagResponse> tags) {
    return new Items(tags);
  }

  static TagActorResponse deleted() {
    return new Deleted();
  }

  static TagActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  static TagActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  static TagActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
