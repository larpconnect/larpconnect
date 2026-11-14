package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Response DTO for AIP-233 batch activity ingress returning partial-success status. */
@Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record BatchInboxActivitiesResponse(
    @JsonProperty("failedRequests") ImmutableMap<String, BatchInboxItemError> failedRequests) {

  public BatchInboxActivitiesResponse {
    failedRequests = ImmutableMap.copyOf(failedRequests);
  }

  @JsonCreator
  public BatchInboxActivitiesResponse(
      @JsonProperty("failedRequests") @Nullable Map<String, BatchInboxItemError> failedRequests) {
    this(failedRequests == null ? ImmutableMap.of() : ImmutableMap.copyOf(failedRequests));
  }
}
