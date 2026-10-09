package com.larpconnect.njall.data.dao.admin;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.AdminRole;
import com.larpconnect.njall.data.domain.AdminUser;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultAdminUserDAO implements AdminUserDAO {

  private static final String HQL_FIND_BY_USERNAME =
      "from AdminUserEntity u left join fetch u.roles where u.username = :username";
  private static final String HQL_LIST =
      "from AdminUserEntity u left join fetch u.roles order by u.username asc";
  private static final String SQL_INSERT_USER =
      """
      INSERT INTO njall_admin.admin_users (username, status)
      VALUES (:username, CAST(:status AS njall_admin.tstatus))
      RETURNING id
      """;
  private static final String SQL_INSERT_ROLE_ASSIGNMENT =
      """
      INSERT INTO njall_admin.admin_role_assignments (admin_user_id, role_id)
      VALUES (:userId, :roleId) ON CONFLICT DO NOTHING
      """;
  private static final String SQL_DELETE_ROLE_ASSIGNMENT =
      """
      DELETE FROM njall_admin.admin_role_assignments
      WHERE admin_user_id = :userId AND role_id = :roleId
      """;

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultAdminUserDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<AdminUser> findById(UUID id) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity = session.find(AdminUserEntity.class, id);
      return Optional.ofNullable(entity).map(DefaultAdminUserDAO::toUser);
    }
  }

  @Override
  public Optional<AdminUser> findByUsername(String username) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity =
          session
              .createQuery(HQL_FIND_BY_USERNAME, AdminUserEntity.class)
              .setParameter("username", username)
              .uniqueResult();
      return Optional.ofNullable(entity).map(DefaultAdminUserDAO::toUser);
    }
  }

  @Override
  public ImmutableList<AdminUser> list() {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entities = session.createQuery(HQL_LIST, AdminUserEntity.class).list();
      return entities.stream()
          .distinct()
          .map(DefaultAdminUserDAO::toUser)
          .collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public AdminUser create(String username, AdminUserStatus status, List<UUID> roleIds) {
    if (status == AdminUserStatus.UNKNOWN) {
      throw new IllegalArgumentException("Cannot create admin user with UNKNOWN status");
    }
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var userId =
            session
                .createNativeQuery(SQL_INSERT_USER, UUID.class)
                .setParameter("username", username)
                .setParameter("status", status.name())
                .getSingleResult();

        for (var roleId : roleIds) {
          session
              .createNativeQuery(SQL_INSERT_ROLE_ASSIGNMENT, Void.class)
              .setParameter("userId", userId)
              .setParameter("roleId", roleId)
              .executeUpdate();
        }
        tx.commit();
        session.clear();
        var entity =
            requireNonNull(
                session.find(AdminUserEntity.class, userId), "Created user entity cannot be null");
        return toUser(entity);
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public AdminUser addRole(UUID userId, UUID roleId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var userEntity = session.find(AdminUserEntity.class, userId);
        if (userEntity == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }
        var roleEntity = session.find(AdminRoleEntity.class, roleId);
        if (roleEntity == null) {
          throw new IllegalArgumentException("Role not found: " + roleId);
        }
        session
            .createNativeQuery(SQL_INSERT_ROLE_ASSIGNMENT, Void.class)
            .setParameter("userId", userId)
            .setParameter("roleId", roleId)
            .executeUpdate();
        tx.commit();
        session.clear();
        var updated =
            requireNonNull(
                session.find(AdminUserEntity.class, userId), "User entity cannot be null");
        return toUser(updated);
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public AdminUser removeRole(UUID userId, UUID roleId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var userEntity = session.find(AdminUserEntity.class, userId);
        if (userEntity == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }
        var roleEntity = session.find(AdminRoleEntity.class, roleId);
        if (roleEntity == null) {
          throw new IllegalArgumentException("Role not found: " + roleId);
        }
        session
            .createNativeQuery(SQL_DELETE_ROLE_ASSIGNMENT, Void.class)
            .setParameter("userId", userId)
            .setParameter("roleId", roleId)
            .executeUpdate();
        tx.commit();
        session.clear();
        var updated =
            requireNonNull(
                session.find(AdminUserEntity.class, userId), "User entity cannot be null");
        return toUser(updated);
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  private static AdminUser toUser(AdminUserEntity entity) {
    var roles =
        entity.getRoles().stream()
            .map(r -> new AdminRole(r.getId(), r.getRoleName()))
            .collect(ImmutableList.toImmutableList());
    return new AdminUser(
        entity.getId(),
        entity.getUsername(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        roles);
  }
}
