package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for multi-tenant studio lookups. */
public non-sealed interface StudioDAO extends DAO<StudioLookup> {

  /**
   * Retrieves an active studio by its unique alias.
   *
   * @param alias The studio alias.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  Optional<StudioLookup> findByAlias(String alias);

  /**
   * Retrieves a studio by its unique alias, optionally including soft-deleted studios.
   *
   * @param alias The studio alias.
   * @param includeDeleted Whether to include soft-deleted studios.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  Optional<StudioLookup> findByAlias(String alias, boolean includeDeleted);

  /**
   * Retrieves a studio by its public studioId, optionally including soft-deleted studios.
   *
   * @param studioId The public studio UUID.
   * @param includeDeleted Whether to include soft-deleted studios.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  Optional<StudioLookup> findById(UUID studioId, boolean includeDeleted);

  /**
   * Lists studios, optionally including soft-deleted studios.
   *
   * @param includeDeleted Whether to include soft-deleted studios.
   * @return Immutable list of studios.
   */
  ImmutableList<StudioLookup> list(boolean includeDeleted);

  /**
   * Creates and persists a new studio lookup record.
   *
   * @param alias The studio alias.
   * @return The persisted studio record.
   */
  StudioLookup create(String alias);

  /**
   * Soft-deletes a studio by setting its deleted_at timestamp.
   *
   * @param studioId The public studio UUID.
   * @return The updated studio record, or empty if not found.
   */
  Optional<StudioLookup> softDelete(UUID studioId);
}
