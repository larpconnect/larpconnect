package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.ContractTag;
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
  @AiContract(
      require = {"$id \\neq \\bot$"},
      tags = {ContractTag.IDEMPOTENT},
      implementationHint = "Finds an entity by its identifier.")
  Uni<T> findById(ID id);

  /**
   * Persists the given entity to the database.
   *
   * @param entity The entity to persist
   * @return A Uni representing the completion of the persistence operation
   */
  @AiContract(
      require = {"$entity \\neq \\bot$"},
      implementationHint = "Persists the given entity to the database.")
  Uni<Void> persist(T entity);
}
