package com.larpconnect.njall.data.dao.studios.events;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable parameter bundle for applying partial updates to a tenanted event.
 *
 * @param eventType Optional updated non-game event classification type.
 * @param title Optional updated event title.
 * @param summary Optional updated description summary.
 * @param clearSummary Whether to clear {@code summary} to null.
 * @param locationId Optional updated location UUID identifier.
 * @param clearLocationId Whether to clear {@code locationId} to null.
 * @param startTime Optional updated start timestamp.
 * @param clearStartTime Whether to clear {@code startTime} to null.
 * @param endTime Optional updated end timestamp.
 * @param clearEndTime Whether to clear {@code endTime} to null.
 */
@Immutable
public record PatchEventParams(
    Optional<EventType> eventType,
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
   * @param eventType Optional updated non-game event classification type.
   * @param title Optional updated event title.
   * @param summary Optional updated description summary.
   * @param locationId Optional updated location UUID identifier.
   * @param startTime Optional updated start timestamp.
   * @param endTime Optional updated end timestamp.
   */
  public PatchEventParams(
      Optional<EventType> eventType,
      Optional<String> title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime) {
    this(eventType, title, summary, false, locationId, false, startTime, false, endTime, false);
  }
}
