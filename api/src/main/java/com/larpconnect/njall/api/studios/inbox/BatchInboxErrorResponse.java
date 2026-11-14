package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Error response DTO returned when all batch items fail or batch request is invalid. */
@Immutable
public record BatchInboxErrorResponse(
    @JsonProperty("code") int code,
    @JsonProperty("message") String message,
    @JsonProperty("failedRequests") ImmutableMap<String, BatchInboxItemError> failedRequests) {

  public BatchInboxErrorResponse {
    failedRequests = ImmutableMap.copyOf(failedRequests);
  }

  @JsonCreator
  public BatchInboxErrorResponse(
      @JsonProperty("code") int code,
      @JsonProperty("message") String message,
      @JsonProperty("failedRequests") @Nullable Map<String, BatchInboxItemError> failedRequests) {
    this(
        code,
        message,
        failedRequests == null ? ImmutableMap.of() : ImmutableMap.copyOf(failedRequests));
  }
}
