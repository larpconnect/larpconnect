package com.larpconnect.njall.data.domain.studios;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.common.DatabaseObject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted 2nd-level CTI game entity extending {@link Event}.
 *
 * @param id The unique game UUID identifier.
 * @param generator The generating actor entity UUID.
 * @param systemId The required game system UUID.
 * @param campaignId Optional UUID of the associated campaign.
 * @param locationId Optional UUID of the associated physical location.
 * @param title The required human-readable title of the game.
 * @param summary Optional description summary from the base entity.
 * @param startTime Optional starting timestamp of the game.
 * @param endTime Optional ending timestamp of the game.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Game(
    UUID id,
    UUID generator,
    UUID systemId,
    Optional<UUID> campaignId,
    Optional<UUID> locationId,
    String title,
    Optional<String> summary,
    Optional<Instant> startTime,
    Optional<Instant> endTime,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Game(
      UUID id,
      UUID generator,
      UUID systemId,
      @Nullable UUID campaignId,
      @Nullable UUID locationId,
      String title,
      @Nullable String summary,
      @Nullable Instant startTime,
      @Nullable Instant endTime,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        generator,
        systemId,
        Optional.ofNullable(campaignId),
        Optional.ofNullable(locationId),
        title,
        Optional.ofNullable(summary),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  /** Returns the immutable {@link EventType#GAME} classification for game entities. */
  public EventType eventType() {
    return EventType.GAME;
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
