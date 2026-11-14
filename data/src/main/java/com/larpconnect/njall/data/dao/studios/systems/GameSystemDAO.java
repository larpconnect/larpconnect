package com.larpconnect.njall.data.dao.studios.systems;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.studios.GameSystem;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted game system catalog operations in user space. */
public interface GameSystemDAO extends DAO<GameSystem> {

  /**
   * Retrieves an active game system by studio tenant UUID and system UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param systemId The game system UUID identifier.
   * @return An Optional containing the game system if found, active, and of type {@code System}.
   */
  Optional<GameSystem> findById(UUID tenantId, UUID systemId);

  /**
   * Retrieves all active game systems for the specified studio tenant ordered alphabetically.
   *
   * @param tenantId The internal tenant UUID.
   * @return An immutable list of active game systems ordered by {@code lower(name) ASC, created_on
   *     DESC}.
   */
  ImmutableList<GameSystem> listAll(UUID tenantId);

  /**
   * Persists a new game system and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param generator The generating actor entity UUID.
   * @param name The required unique game system name within the tenant.
   * @param summary Optional description summary from the base entity.
   * @return The persisted GameSystem domain record.
   * @throws IllegalArgumentException if {@code name} is blank or {@code generator} is not an active
   *     Actor.
   * @throws IllegalStateException if a system with the same case-insensitive name already exists in
   *     the tenant.
   */
  GameSystem create(UUID tenantId, UUID generator, String name, Optional<String> summary);

  /**
   * Applies partial updates to an active game system.
   *
   * @param tenantId The internal tenant UUID.
   * @param systemId The game system UUID identifier.
   * @param params Partial field updates and explicit null clearing flags.
   * @return An Optional containing the updated GameSystem if found, active, and of type {@code
   *     System}.
   * @throws IllegalArgumentException if {@code params.name()} is present and blank.
   * @throws IllegalStateException if {@code params.name()} collides case-insensitively with another
   *     system in the tenant.
   */
  Optional<GameSystem> patch(UUID tenantId, UUID systemId, PatchGameSystemParams params);

  /**
   * Soft deletes a game system by updating its base entity deleted_on timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param systemId The game system UUID identifier.
   * @return true if the active {@code System} entity was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID systemId);

  @Override
  default Optional<GameSystem> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct system lookup requires tenant context; use findById(tenantId, systemId)");
  }

  @Override
  default ImmutableList<GameSystem> list() {
    throw new UnsupportedOperationException(
        "Direct un-tenanted system listing is not permitted in user space; use listAll(tenantId)");
  }
}
