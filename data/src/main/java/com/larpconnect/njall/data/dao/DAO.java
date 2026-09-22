package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.DatabaseObject;
import java.util.Optional;
import java.util.UUID;

/**
 * Base sealed Data Access Object interface providing common read-only querying primitives.
 *
 * @param <T> The database object type managed by this DAO.
 */
public sealed interface DAO<T extends DatabaseObject>
    permits ServerDAO, AdminUserDAO, AdminRoleDAO, StudioDAO {

  /**
   * Retrieves an entity by its unique UUID identifier.
   *
   * @param id The entity UUID identifier.
   * @return An {@link Optional} containing the entity if found, otherwise empty.
   */
  Optional<T> findById(UUID id);

  /**
   * Retrieves all instances of the entity type.
   *
   * @return An immutable list containing all entity instances.
   */
  ImmutableList<T> list();
}
