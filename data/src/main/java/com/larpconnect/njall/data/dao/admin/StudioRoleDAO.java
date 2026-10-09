package com.larpconnect.njall.data.dao.admin;

import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.DefaultStudioRole;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for managing system-wide default studio roles. */
public interface StudioRoleDAO extends DAO<DefaultStudioRole> {

  /**
   * Retrieves a default studio role by its unique name.
   *
   * @param name The role name.
   * @return An Optional containing the role if found, otherwise empty.
   */
  Optional<DefaultStudioRole> findByName(String name);

  /**
   * Creates and persists a new default studio role.
   *
   * @param name The role name.
   * @return The persisted default studio role record.
   */
  DefaultStudioRole create(String name);

  /**
   * Updates the name of an existing default studio role.
   *
   * @param id The role UUID identifier.
   * @param name The updated role name.
   * @return An Optional containing the updated role, or empty if not found.
   */
  Optional<DefaultStudioRole> update(UUID id, String name);
}
