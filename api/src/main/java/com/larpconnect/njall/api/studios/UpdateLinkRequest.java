package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for updating an existing studio link conforming to AIP-134. */
@Immutable
public record UpdateLinkRequest(
    @JsonProperty("linkType") Optional<String> linkType,
    @JsonProperty("url") Optional<String> url,
    @JsonProperty("mediaType") Optional<String> mediaType,
    @JsonProperty("summary") Optional<String> summary) {

  @JsonCreator
  public UpdateLinkRequest(
      @JsonProperty("linkType") @Nullable String linkType,
      @JsonProperty("url") @Nullable String url,
      @JsonProperty("mediaType") @Nullable String mediaType,
      @JsonProperty("summary") @Nullable String summary) {
    this(
        Optional.ofNullable(linkType),
        Optional.ofNullable(url),
        Optional.ofNullable(mediaType),
        Optional.ofNullable(summary));
  }
}
