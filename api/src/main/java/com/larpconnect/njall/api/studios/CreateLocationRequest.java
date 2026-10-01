package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio location. */
@Immutable
public record CreateLocationRequest(
    @JsonProperty("name") String name, @JsonProperty("summary") Optional<String> summary) {

  public CreateLocationRequest(String name) {
    this(name, Optional.empty());
  }

  @JsonCreator
  public CreateLocationRequest(
      @JsonProperty("name") String name, @JsonProperty("summary") @Nullable String summary) {
    this(name, Optional.ofNullable(summary));
  }
}
