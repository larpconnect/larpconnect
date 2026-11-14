package com.larpconnect.njall.data.dao.studios.campaigns;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable parameter bundle for applying partial updates to a tenanted campaign.
 *
 * @param title Optional updated campaign title.
 * @param summary Optional updated description summary.
 * @param clearSummary Whether to clear {@code summary} to null.
 * @param systemId Optional updated game system UUID identifier.
 * @param clearSystem Whether to clear {@code systemId} to null.
 */
@Immutable
public record PatchCampaignParams(
    Optional<String> title,
    Optional<String> summary,
    boolean clearSummary,
    Optional<UUID> systemId,
    boolean clearSystem) {

  /**
   * Constructs partial update parameters without explicit null clearing flags.
   *
   * @param title Optional updated campaign title.
   * @param summary Optional updated description summary.
   * @param systemId Optional updated game system UUID identifier.
   */
  public PatchCampaignParams(
      Optional<String> title, Optional<String> summary, Optional<UUID> systemId) {
    this(title, summary, false, systemId, false);
  }
}
