package org.larpconnect.data;

import java.util.Optional;
import java.util.UUID;

/** DAO interface for managing {@link TestTable} objects. */
public interface TestTableDao {
  /**
   * Saves or merges the given test entity into the database.
   *
   * @param entity The entity to save.
   */
  void save(TestTable entity);

  /**
   * Finds a test entity by its UUID.
   *
   * @param id The UUID of the entity.
   * @return Optional containing the entity if found, otherwise empty.
   */
  Optional<TestTable> findById(UUID id);
}
