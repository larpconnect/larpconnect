package com.larpconnect.njall.data.dao.studios.events;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.studios.Event;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted non-game event operations in user space. */
public interface EventDAO extends DAO<Event> {

  /**
   * Retrieves an active non-game event by studio tenant UUID and event UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param eventId The event UUID identifier.
   * @return An Optional containing the event if found, active, and of type {@code Event}.
   */
  Optional<Event> findById(UUID tenantId, UUID eventId);

  /**
   * Retrieves all active non-game events for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @return An immutable list of active events ordered chronologically.
   */
  ImmutableList<Event> listAll(UUID tenantId);

  /**
   * Persists a new non-game event and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param generator The generating actor entity UUID.
   * @param eventType The non-game event classification type.
   * @param title The required event title.
   * @param summary Optional description summary from the base entity.
   * @param locationId Optional location UUID identifier.
   * @param startTime Optional start timestamp.
   * @param endTime Optional end timestamp.
   * @return The persisted Event domain record.
   * @throws IllegalArgumentException if {@code eventType} is {@link EventType#GAME}, generator is
   *     not an active Actor, or locationId does not reference an active Location.
   */
  Event create(
      UUID tenantId,
      UUID generator,
      EventType eventType,
      String title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime);

  /**
   * Applies partial updates to an active non-game event.
   *
   * @param tenantId The internal tenant UUID.
   * @param eventId The event UUID identifier.
   * @param params Partial field updates and explicit null clearing flags.
   * @return An Optional containing the updated Event if found, active, and of type {@code Event}.
   * @throws IllegalArgumentException if {@code params.eventType()} is {@link EventType#GAME} or
   *     {@code params.locationId()} does not reference an active Location.
   */
  Optional<Event> patch(UUID tenantId, UUID eventId, PatchEventParams params);

  /**
   * Soft deletes a non-game event by updating its base entity deleted_on timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param eventId The event UUID identifier.
   * @return true if the active {@code Event} entity was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID eventId);

  @Override
  default Optional<Event> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct event lookup requires tenant context; use findById(tenantId, eventId)");
  }

  @Override
  default ImmutableList<Event> list() {
    throw new UnsupportedOperationException(
        "Direct un-tenanted event listing is not permitted in user space; use listAll(tenantId)");
  }
}
