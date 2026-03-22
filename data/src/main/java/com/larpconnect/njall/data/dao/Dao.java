package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.DatabaseObject;
import io.smallrye.mutiny.Uni;

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
   * @param id The identifier
   * @return A Uni containing the entity, or null if not found
   */
  Uni<T> findById(String serverId, ID id);

  /**
   * Persists the given entity to the database.
   *
   * @param entity The entity to persist
   * @return A Uni representing the completion of the persistence operation
   */
  Uni<Void> persist(String serverId, T entity);
}
