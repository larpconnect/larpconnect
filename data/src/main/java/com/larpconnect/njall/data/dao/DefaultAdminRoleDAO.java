package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.AdminRole;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultAdminRoleDAO implements AdminRoleDAO {

  private static final String HQL_FIND_BY_ROLE_NAME =
      "from AdminRoleEntity where roleName = :roleName";
  private static final String HQL_LIST = "from AdminRoleEntity order by roleName asc";
  private static final String SQL_INSERT_ROLE =
      "INSERT INTO njall_admin.admin_roles (role_name) VALUES (:roleName) RETURNING id";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultAdminRoleDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<AdminRole> findById(UUID id) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity = session.find(AdminRoleEntity.class, id);
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public Optional<AdminRole> findByRoleName(String roleName) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity =
          session
              .createQuery(HQL_FIND_BY_ROLE_NAME, AdminRoleEntity.class)
              .setParameter("roleName", roleName)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public ImmutableList<AdminRole> list() {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entities = session.createQuery(HQL_LIST, AdminRoleEntity.class).list();
      return entities.stream().map(this::toRole).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public AdminRole create(String roleName) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var id =
            session
                .createNativeQuery(SQL_INSERT_ROLE, UUID.class)
                .setParameter("roleName", roleName)
                .getSingleResult();
        tx.commit();
        return new AdminRole(id, roleName);
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

  private AdminRole toRole(AdminRoleEntity entity) {
    return new AdminRole(entity.getId(), entity.getRoleName());
  }
}
