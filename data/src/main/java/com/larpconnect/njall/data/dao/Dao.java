package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.DatabaseObject;
import io.smallrye.mutiny.Uni;
import javax.annotation.Nullable;

/**
 * Base Data Access Object interface for all database entities.
 *
 * @param <T> The entity type
 * @param <ID> The type of the identifier
 */
public interface Dao<T extends DatabaseObject, ID> {

  /**
   * Finds an entity by its identifier.
   *
   * @param serverId The specific server id/tenant id (can be null for base/public interactions).
   * @param id The identifier
   * @return A Uni containing the entity, or null if not found
   */
  Uni<T> findById(@Nullable String serverId, ID id);

  /**
   * Persists the given entity to the database.
   *
   * @param serverId The specific server id/tenant id (can be null for base/public interactions).
   * @param entity The entity to persist
   * @return A Uni representing the completion of the persistence operation
   */
  Uni<Void> persist(@Nullable String serverId, T entity);
}
