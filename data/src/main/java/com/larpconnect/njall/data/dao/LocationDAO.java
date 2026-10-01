package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.Location;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted venue location operations in user space. */
public non-sealed interface LocationDAO extends DAO<Location> {

  /**
   * Retrieves an active location by studio tenant UUID and location UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The location UUID identifier.
   * @return An Optional containing the location if found and active, otherwise empty.
   */
  Optional<Location> findById(UUID tenantId, UUID locationId);

  /**
   * Persists a new location and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param name The human-readable location name.
   * @param summary Optional human-readable description.
   * @return The persisted Location domain record.
   */
  Location create(UUID tenantId, String name, Optional<String> summary);

  /**
   * Applies partial updates to an active location.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The location UUID identifier.
   * @param name Optional updated location name.
   * @param summary Optional updated summary.
   * @return An Optional containing the updated Location if found and active, otherwise empty.
   */
  Optional<Location> patch(
      UUID tenantId, UUID locationId, Optional<String> name, Optional<String> summary);

  /**
   * Soft deletes a location by updating its base entity deleted_on timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The location UUID identifier.
   * @return true if the location was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID locationId);

  @Override
  default Optional<Location> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct location lookup requires tenant context; use findById(tenantId, locationId)");
  }
}
