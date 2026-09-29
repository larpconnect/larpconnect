package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio link. */
@Immutable
public record CreateLinkRequest(
    @JsonProperty("linkType") String linkType,
    @JsonProperty("url") String url,
    @JsonProperty("mediaType") Optional<String> mediaType,
    @JsonProperty("summary") Optional<String> summary) {

  public CreateLinkRequest(String linkType, String url) {
    this(linkType, url, Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public CreateLinkRequest(
      @JsonProperty("linkType") String linkType,
      @JsonProperty("url") String url,
      @JsonProperty("mediaType") @Nullable String mediaType,
      @JsonProperty("summary") @Nullable String summary) {
    this(linkType, url, Optional.ofNullable(mediaType), Optional.ofNullable(summary));
  }
}
