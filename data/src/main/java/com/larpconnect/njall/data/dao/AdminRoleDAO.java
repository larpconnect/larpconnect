package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.AdminRole;
import java.util.Optional;

/** Data Access Object for administrative roles. */
public non-sealed interface AdminRoleDAO extends DAO<AdminRole> {

  /**
   * Retrieves a role by its unique canonical role name.
   *
   * @param roleName The role name.
   * @return An Optional containing the role if found, otherwise empty.
   */
  Optional<AdminRole> findByRoleName(String roleName);

  /**
   * Creates and persists a new administrative role.
   *
   * @param roleName The canonical role name.
   * @return The persisted role record.
   */
  AdminRole create(String roleName);
}
