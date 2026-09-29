package com.larpconnect.njall.api.admin.studios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio. */
@Immutable
public record CreateStudioRequest(
    @JsonProperty("alias") String alias, @JsonProperty("name") Optional<String> name) {

  public CreateStudioRequest(String alias) {
    this(alias, Optional.empty());
  }

  @JsonCreator
  public CreateStudioRequest(
      @JsonProperty("alias") String alias, @JsonProperty("name") @Nullable String name) {
    this(alias, Optional.ofNullable(name));
  }
}
