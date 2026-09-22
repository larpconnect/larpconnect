package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.AdminUser;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for administrative users and role assignments. */
public non-sealed interface AdminUserDAO extends DAO<AdminUser> {

  /**
   * Retrieves an admin user by username.
   *
   * @param username The username.
   * @return An Optional containing the user if found, otherwise empty.
   */
  Optional<AdminUser> findByUsername(String username);

  /**
   * Creates and persists a new administrative user with optional initial role assignments.
   *
   * @param username The username.
   * @param status The initial user status.
   * @param roleIds The list of initial role IDs to assign.
   * @return The persisted user record with assigned roles.
   */
  AdminUser create(String username, AdminUserStatus status, List<UUID> roleIds);

  /**
   * Idempotently assigns a role to an administrative user.
   *
   * @param userId The user UUID.
   * @param roleId The role UUID.
   * @return The updated user record.
   */
  AdminUser addRole(UUID userId, UUID roleId);

  /**
   * Idempotently removes a role from an administrative user.
   *
   * @param userId The user UUID.
   * @param roleId The role UUID.
   * @return The updated user record.
   */
  AdminUser removeRole(UUID userId, UUID roleId);
}
