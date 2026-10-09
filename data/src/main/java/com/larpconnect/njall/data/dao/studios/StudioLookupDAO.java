package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.DeletionFilter;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for multi-tenant studio lookups in the admin plane. */
public interface StudioLookupDAO extends DAO<StudioLookup> {

  /**
   * Retrieves an active studio by its unique alias.
   *
   * @param alias The studio alias.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  default Optional<StudioLookup> findByAlias(String alias) {
    return findByAlias(alias, DeletionFilter.ACTIVE_ONLY);
  }

  /**
   * Retrieves a studio by its unique alias according to the specified deletion filter.
   *
   * @param alias The studio alias.
   * @param filter The deletion filter strategy.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  Optional<StudioLookup> findByAlias(String alias, DeletionFilter filter);

  /**
   * Retrieves a studio by its public studioId according to the specified deletion filter.
   *
   * @param studioId The public studio UUID.
   * @param filter The deletion filter strategy.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  Optional<StudioLookup> findById(UUID studioId, DeletionFilter filter);

  /**
   * Lists studios according to the specified deletion filter.
   *
   * @param filter The deletion filter strategy.
   * @return Immutable list of studios.
   */
  ImmutableList<StudioLookup> list(DeletionFilter filter);

  @Override
  default Optional<StudioLookup> findById(UUID studioId) {
    return findById(studioId, DeletionFilter.ACTIVE_ONLY);
  }

  @Override
  default ImmutableList<StudioLookup> list() {
    return list(DeletionFilter.ACTIVE_ONLY);
  }

  /**
   * Creates and persists a new studio lookup record with default name equal to alias.
   *
   * @param alias The studio alias.
   * @return The persisted studio record.
   */
  default StudioLookup create(String alias) {
    return create(alias, alias);
  }

  /**
   * Creates and persists a new studio record in the user schema and its corresponding lookup record
   * in the admin schema.
   *
   * @param alias The studio alias.
   * @param name The human-readable studio name.
   * @return The persisted studio lookup record.
   */
  StudioLookup create(String alias, String name);

  /**
   * Soft-deletes a studio by setting its deleted_at timestamp.
   *
   * @param studioId The public studio UUID.
   * @return The updated studio record, or empty if not found.
   */
  Optional<StudioLookup> softDelete(UUID studioId);
}
