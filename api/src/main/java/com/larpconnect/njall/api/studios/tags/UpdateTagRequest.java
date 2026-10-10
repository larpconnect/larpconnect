package com.larpconnect.njall.api.studios.tags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for updating an existing studio hashtag conforming to AIP-134. */
@Immutable
public record UpdateTagRequest(
    @JsonProperty("tag") Optional<String> tag, @JsonProperty("summary") Optional<String> summary) {

  @JsonCreator
  public UpdateTagRequest(
      @JsonProperty("tag") @Nullable String tag,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(tag), Optional.ofNullable(summary));
  }
}
