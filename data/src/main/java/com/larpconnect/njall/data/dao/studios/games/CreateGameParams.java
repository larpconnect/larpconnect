package com.larpconnect.njall.data.dao.studios.games;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable parameter bundle for creating a tenanted game entity.
 *
 * @param generator The generating actor entity UUID.
 * @param systemId The required active system UUID.
 * @param campaignId Optional active campaign UUID.
 * @param title The required human-readable game title.
 * @param summary Optional description summary for the base entity.
 * @param locationId Optional active location UUID.
 * @param startTime Optional game start timestamp.
 * @param endTime Optional game end timestamp (must be on or after {@code startTime}).
 */
@Immutable
public record CreateGameParams(
    UUID generator,
    UUID systemId,
    Optional<UUID> campaignId,
    String title,
    Optional<String> summary,
    Optional<UUID> locationId,
    Optional<Instant> startTime,
    Optional<Instant> endTime) {}
