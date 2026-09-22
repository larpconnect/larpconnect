package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.StudioLookup;

/** Response protocol emitted by the studio admin actor. */
public sealed interface StudioAdminResponse {

  /** Represents a single retrieved or created studio. */
  record StudioSingle(StudioLookup studio) implements StudioAdminResponse {}

  /** Represents a list of retrieved studios. */
  record StudioList(ImmutableList<StudioLookup> studios) implements StudioAdminResponse {}

  /** Indicates that the requested studio was not found. */
  record NotFound(String message) implements StudioAdminResponse {}

  /** Indicates that the studio alias already exists (conflict). */
  record Conflict(String message) implements StudioAdminResponse {}

  /** Indicates a malformed request or validation failure. */
  record BadRequest(String message) implements StudioAdminResponse {}

  /** Indicates an unexpected internal failure during the operation. */
  record Failure(String message) implements StudioAdminResponse {}

  static StudioAdminResponse single(StudioLookup studio) {
    return new StudioSingle(studio);
  }

  static StudioAdminResponse list(ImmutableList<StudioLookup> studios) {
    return new StudioList(studios);
  }

  static StudioAdminResponse notFound(String message) {
    return new NotFound(message);
  }

  static StudioAdminResponse conflict(String message) {
    return new Conflict(message);
  }

  static StudioAdminResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static StudioAdminResponse failure(String message) {
    return new Failure(message);
  }
}
