package com.larpconnect.njall.api.studios.tags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating or resolving a studio hashtag. */
@Immutable
public record CreateTagRequest(
    @JsonProperty("generator") String generator,
    @JsonProperty("tag") String tag,
    @JsonProperty("summary") Optional<String> summary) {

  public CreateTagRequest(String generator, String tag) {
    this(generator, tag, Optional.empty());
  }

  @JsonCreator
  public CreateTagRequest(
      @JsonProperty("generator") String generator,
      @JsonProperty("tag") String tag,
      @JsonProperty("summary") @Nullable String summary) {
    this(generator, tag, Optional.ofNullable(summary));
  }
}
