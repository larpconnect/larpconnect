package com.larpconnect.njall.api.studios.campaigns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a studio campaign (AIP-134). */
@Immutable
public record UpdateCampaignRequest(
    @JsonProperty("title") Optional<String> title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("system") Optional<String> system) {

  /** Constructs an empty update campaign request with no fields targeted. */
  public UpdateCampaignRequest() {
    this(Optional.empty(), Optional.empty(), Optional.empty());
  }

  /**
   * Jackson deserialization constructor normalizing nullable JSON fields.
   *
   * @param title Optional updated campaign title.
   * @param summary Optional updated campaign summary.
   * @param system Optional updated game system UUID or canonical studio System URI.
   */
  @JsonCreator
  public UpdateCampaignRequest(
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("system") @Nullable String system) {
    this(Optional.ofNullable(title), Optional.ofNullable(summary), Optional.ofNullable(system));
  }
}
