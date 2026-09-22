package com.larpconnect.njall.data.dao;

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
import org.hibernate.Session;
import org.hibernate.SessionFactory;

final class DefaultAdminUserDAO implements AdminUserDAO {

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultAdminUserDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<AdminUser> findById(UUID id) {
    requireNonNull(id, "id cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity = session.find(AdminUserEntity.class, id);
      return Optional.ofNullable(entity).map(this::toUser);
    }
  }

  @Override
  public Optional<AdminUser> findByUsername(String username) {
    requireNonNull(username, "username cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = "from AdminUserEntity u left join fetch u.roles where u.username = :username";
      var entity =
          session
              .createQuery(hql, AdminUserEntity.class)
              .setParameter("username", username)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toUser);
    }
  }

  @Override
  public ImmutableList<AdminUser> list() {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = "from AdminUserEntity u left join fetch u.roles order by u.username asc";
      var entities = session.createQuery(hql, AdminUserEntity.class).list();
      return entities.stream()
          .distinct()
          .map(this::toUser)
          .collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public AdminUser create(String username, AdminUserStatus status, List<UUID> roleIds) {
    requireNonNull(username, "username cannot be null");
    requireNonNull(status, "status cannot be null");
    requireNonNull(roleIds, "roleIds cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var insertUserSql =
            "INSERT INTO njall_admin.admin_users (username, status) "
                + "VALUES (:username, CAST(:status AS njall_admin.tstatus)) "
                + "RETURNING id";
        var userId =
            session
                .createNativeQuery(insertUserSql, UUID.class)
                .setParameter("username", username)
                .setParameter("status", status.name())
                .getSingleResult();

        for (var roleId : roleIds) {
          assignRole(session, userId, roleId);
        }
        tx.commit();
        return findById(userId).orElseThrow();
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public AdminUser addRole(UUID userId, UUID roleId) {
    requireNonNull(userId, "userId cannot be null");
    requireNonNull(roleId, "roleId cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        verifyUserExists(session, userId);
        verifyRoleExists(session, roleId);
        assignRole(session, userId, roleId);
        tx.commit();
        return findById(userId).orElseThrow();
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public AdminUser removeRole(UUID userId, UUID roleId) {
    requireNonNull(userId, "userId cannot be null");
    requireNonNull(roleId, "roleId cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        verifyUserExists(session, userId);
        verifyRoleExists(session, roleId);
        var deleteSql =
            "DELETE FROM njall_admin.admin_role_assignments "
                + "WHERE admin_user_id = :userId AND role_id = :roleId";
        session
            .createNativeQuery(deleteSql, Void.class)
            .setParameter("userId", userId)
            .setParameter("roleId", roleId)
            .executeUpdate();
        tx.commit();
        return findById(userId).orElseThrow();
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  private void verifyUserExists(Session session, UUID userId) {
    var userEntity = session.find(AdminUserEntity.class, userId);
    if (userEntity == null) {
      throw new IllegalArgumentException("User not found: " + userId);
    }
  }

  private void verifyRoleExists(Session session, UUID roleId) {
    var roleEntity = session.find(AdminRoleEntity.class, roleId);
    if (roleEntity == null) {
      throw new IllegalArgumentException("Role not found: " + roleId);
    }
  }

  private void assignRole(Session session, UUID userId, UUID roleId) {
    var insertRoleSql =
        "INSERT INTO njall_admin.admin_role_assignments (admin_user_id, role_id) "
            + "VALUES (:userId, :roleId) ON CONFLICT DO NOTHING";
    session
        .createNativeQuery(insertRoleSql, Void.class)
        .setParameter("userId", userId)
        .setParameter("roleId", roleId)
        .executeUpdate();
  }

  private AdminUser toUser(AdminUserEntity entity) {
    var roles =
        entity.getRoles().stream()
            .map(r -> AdminRole.of(r.getId(), r.getRoleName()))
            .collect(ImmutableList.toImmutableList());
    return AdminUser.of(
        entity.getId(),
        entity.getUsername(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        roles);
  }
}
