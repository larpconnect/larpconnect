package com.larpconnect.njall.api.studios.inbox;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;

/** Response protocol emitted by {@link InboxActor}. */
public sealed interface InboxActorResponse {

  /** Indicates the activity was accepted and queued or deduplicated. */
  @Immutable
  record Success() implements InboxActorResponse {}

  /** Indicates an AIP-233 batch request completed with partial success (at least 1 accepted). */
  @Immutable
  record BatchSuccess(BatchInboxActivitiesResponse response) implements InboxActorResponse {}

  /** Indicates a single or batch processing failure. */
  @Immutable
  record Failure(int statusCode, String message, Optional<BatchInboxErrorResponse> batchError)
      implements InboxActorResponse {

    public Failure(int statusCode, String message) {
      this(statusCode, message, Optional.empty());
    }
  }
}
