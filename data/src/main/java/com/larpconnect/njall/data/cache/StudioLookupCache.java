package com.larpconnect.njall.data.cache;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.Optional;
import java.util.UUID;

/** Non-blocking in-memory cache for multi-tenant studio lookups. */
public interface StudioLookupCache {

  /**
   * Retrieves a studio lookup by its public studio UUID.
   *
   * @param studioId The public studio UUID.
   * @return An Optional containing the studio lookup if found and active, otherwise empty.
   */
  Optional<StudioLookup> findById(UUID studioId);

  /**
   * Retrieves a studio lookup by its unique alias.
   *
   * @param alias The studio alias.
   * @return An Optional containing the studio lookup if found and active, otherwise empty.
   */
  Optional<StudioLookup> findByAlias(String alias);

  /**
   * Retrieves a studio lookup by mixed identifier (either public UUID string or alias).
   *
   * @param idOrAlias The public studio UUID string or alias.
   * @return An Optional containing the studio lookup if found and active, otherwise empty.
   */
  Optional<StudioLookup> findByIdOrAlias(String idOrAlias);

  /**
   * Lists all active studios currently held in the cache.
   *
   * @return Immutable list of active studio lookups.
   */
  ImmutableList<StudioLookup> listActive();

  /** Synchronously triggers an immediate reload of the cache from the underlying data source. */
  void refresh();
}
