package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.Event;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted event operations in user space. */
public interface EventDAO extends DAO<Event> {

  /**
   * Retrieves an active event by studio tenant UUID and event UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param eventId The event UUID identifier.
   * @return An Optional containing the event if found and active, otherwise empty.
   */
  Optional<Event> findById(UUID tenantId, UUID eventId);

  /**
   * Retrieves all active events for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @return An immutable list of active events ordered chronologically.
   */
  ImmutableList<Event> listAll(UUID tenantId);

  /**
   * Persists a new event and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param title The required event title.
   * @param summary Optional description summary from the base entity.
   * @param locationId Optional location UUID identifier.
   * @param startTime Optional start timestamp.
   * @param endTime Optional end timestamp.
   * @return The persisted Event domain record.
   */
  Event create(
      UUID tenantId,
      String title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime);

  /**
   * Applies partial updates to an active event.
   *
   * @param tenantId The internal tenant UUID.
   * @param eventId The event UUID identifier.
   * @param title Optional updated title.
   * @param summary Optional updated summary.
   * @param locationId Optional updated location UUID identifier.
   * @param startTime Optional updated start timestamp.
   * @param endTime Optional updated end timestamp.
   * @return An Optional containing the updated Event if found and active, otherwise empty.
   */
  Optional<Event> patch(
      UUID tenantId,
      UUID eventId,
      Optional<String> title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime);

  /**
   * Soft deletes an event by updating its base entity deleted_on timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param eventId The event UUID identifier.
   * @return true if the event was found and soft-deleted, false otherwise.
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
