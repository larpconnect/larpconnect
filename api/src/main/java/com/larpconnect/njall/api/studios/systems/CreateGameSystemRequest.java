package com.larpconnect.njall.api.studios.systems;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a game system. */
@Immutable
public record CreateGameSystemRequest(
    @JsonProperty("generator") String generator,
    @JsonProperty("name") String name,
    @JsonProperty("summary") Optional<String> summary) {

  /** Creates a game system request without a summary. */
  public CreateGameSystemRequest(String generator, String name) {
    this(generator, name, Optional.empty());
  }

  /**
   * Jackson creator normalizing nullable JSON fields and converting summary to {@link Optional}.
   */
  @JsonCreator
  public CreateGameSystemRequest(
      @JsonProperty("generator") @Nullable String generator,
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(Strings.nullToEmpty(generator), Strings.nullToEmpty(name), Optional.ofNullable(summary));
  }
}
