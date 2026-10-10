package com.larpconnect.njall.api.studios.tags;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** Request payload for batch creating studio hashtags conforming to AIP-233. */
@Immutable
public record BatchCreateTagsRequest(
    @JsonProperty("requests") @Nullable ImmutableList<CreateTagRequest> requests) {

  public BatchCreateTagsRequest {
    requests = requests == null ? ImmutableList.of() : requests;
  }
}
