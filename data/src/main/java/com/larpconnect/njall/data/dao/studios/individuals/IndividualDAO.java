package com.larpconnect.njall.data.dao.studios.individuals;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.Individual;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted individual operations in user space. */
public interface IndividualDAO extends DAO<Individual> {

  /**
   * Retrieves an active individual by studio tenant UUID and individual UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param individualId The individual UUID identifier.
   * @return An Optional containing the individual if found and active, otherwise empty.
   */
  Optional<Individual> findById(UUID tenantId, UUID individualId);

  /**
   * Persists a new individual and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param name The required individual name.
   * @param summary The optional base entity summary.
   * @return The newly persisted immutable Individual domain record.
   */
  Individual create(UUID tenantId, String name, Optional<String> summary);

  /**
   * Applies partial updates to an active individual within the studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param individualId The individual UUID identifier.
   * @param name Optional replacement name.
   * @param summary Optional replacement summary.
   * @return An Optional containing the updated Individual if found and active, otherwise empty.
   */
  Optional<Individual> patch(
      UUID tenantId, UUID individualId, Optional<String> name, Optional<String> summary);

  /**
   * Performs soft deletion on an individual by recording the current timestamp on the base entity.
   *
   * @param tenantId The internal tenant UUID.
   * @param individualId The individual UUID identifier.
   * @return true if the active individual was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID individualId);

  @Override
  default Optional<Individual> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct individual lookup requires tenant context; use findById(tenantId, individualId)");
  }

  @Override
  default ImmutableList<Individual> list() {
    throw new UnsupportedOperationException(
        "Individual collection listing is not permitted; list endpoint is not supported");
  }
}
