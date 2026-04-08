package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.DatabaseObject;
import io.smallrye.mutiny.Uni;

/**
 * Establishes a unified, non-blocking persistence contract for the application.
 *
 * <p>By abstracting data access behind this interface, the business logic remains entirely
 * decoupled from the specific mechanics of Hibernate Reactive or the underlying PostgreSQL schema.
 * This separation of concerns allows for independent testing of the API layer via mocked DAOs and
 * ensures that database interactions remain consistently reactive and bounded within Mutiny {@link
 * Uni} pipelines to prevent event loop blocking.
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
  Uni<T> findById(ID id);

  /**
   * Persists the given entity to the database.
   *
   * @param entity The entity to persist
   * @return A Uni representing the completion of the persistence operation
   */
  Uni<Void> persist(T entity);
}
