package com.larpconnect.njall.api.studios.campaigns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio campaign. */
@Immutable
public record CreateCampaignRequest(
    @JsonProperty("generator") String generator,
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("system") Optional<String> system) {

  /**
   * Constructs a create campaign request with required fields only.
   *
   * @param generator Generating actor UUID or canonical studio Actor URI.
   * @param title Campaign title.
   */
  public CreateCampaignRequest(String generator, String title) {
    this(generator, title, Optional.empty(), Optional.empty());
  }

  /**
   * Jackson deserialization constructor normalizing nullable JSON fields.
   *
   * @param generator Generating actor UUID or canonical studio Actor URI.
   * @param title Campaign title.
   * @param summary Optional campaign summary.
   * @param system Optional game system UUID or canonical studio System URI.
   */
  @JsonCreator
  public CreateCampaignRequest(
      @JsonProperty("generator") @Nullable String generator,
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("system") @Nullable String system) {
    this(
        Strings.nullToEmpty(generator),
        Strings.nullToEmpty(title),
        Optional.ofNullable(summary),
        Optional.ofNullable(system));
  }
}
