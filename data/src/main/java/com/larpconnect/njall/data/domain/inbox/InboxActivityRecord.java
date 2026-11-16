package com.larpconnect.njall.data.domain.inbox;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain representation of an accepted inbox activity deduplication record.
 *
 * @param tenantId The studio tenant UUID.
 * @param targetActorId The recipient actor UUID.
 * @param activityId The unique activity URI / ID string.
 * @param receivedOn Timestamp when the activity was accepted and queued.
 * @param expiresOn Timestamp when the deduplication record expires (8-day window).
 */
@Immutable
public record InboxActivityRecord(
    UUID tenantId, UUID targetActorId, String activityId, Instant receivedOn, Instant expiresOn) {}
