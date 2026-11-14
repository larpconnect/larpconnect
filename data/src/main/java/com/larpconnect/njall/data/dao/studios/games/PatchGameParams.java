package com.larpconnect.njall.data.dao.studios.games;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable parameter bundle for applying partial updates to a tenanted game entity.
 *
 * @param systemId Optional updated active system UUID ({@code systemId} cannot be cleared).
 * @param campaignId Optional updated active campaign UUID.
 * @param clearCampaign Whether to clear {@code campaignId} to null.
 * @param title Optional updated game title.
 * @param summary Optional updated description summary.
 * @param clearSummary Whether to clear {@code summary} to null.
 * @param locationId Optional updated active location UUID.
 * @param clearLocationId Whether to clear {@code locationId} to null.
 * @param startTime Optional updated start timestamp.
 * @param clearStartTime Whether to clear {@code startTime} to null.
 * @param endTime Optional updated end timestamp.
 * @param clearEndTime Whether to clear {@code endTime} to null.
 */
@Immutable
public record PatchGameParams(
    Optional<UUID> systemId,
    Optional<UUID> campaignId,
    boolean clearCampaign,
    Optional<String> title,
    Optional<String> summary,
    boolean clearSummary,
    Optional<UUID> locationId,
    boolean clearLocationId,
    Optional<Instant> startTime,
    boolean clearStartTime,
    Optional<Instant> endTime,
    boolean clearEndTime) {

  /**
   * Constructs partial update parameters without explicit null clearing flags.
   *
   * @param systemId Optional updated active system UUID.
   * @param campaignId Optional updated active campaign UUID.
   * @param title Optional updated game title.
   * @param summary Optional updated description summary.
   * @param locationId Optional updated active location UUID.
   * @param startTime Optional updated start timestamp.
   * @param endTime Optional updated end timestamp.
   */
  public PatchGameParams(
      Optional<UUID> systemId,
      Optional<UUID> campaignId,
      Optional<String> title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime) {
    this(
        systemId,
        campaignId,
        false,
        title,
        summary,
        false,
        locationId,
        false,
        startTime,
        false,
        endTime,
        false);
  }
}
