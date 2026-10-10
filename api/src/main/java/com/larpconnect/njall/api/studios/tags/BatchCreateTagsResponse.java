package com.larpconnect.njall.api.studios.tags;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** Response payload for batch creating studio hashtags conforming to AIP-233. */
@Immutable
public record BatchCreateTagsResponse(
    @JsonProperty("tags") @Nullable ImmutableList<TagResponse> tags) {

  public BatchCreateTagsResponse {
    tags = tags == null ? ImmutableList.of() : tags;
  }
}
