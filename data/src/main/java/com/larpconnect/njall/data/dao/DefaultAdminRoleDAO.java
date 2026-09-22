package com.larpconnect.njall.data.dao;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.AdminRole;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultAdminRoleDAO implements AdminRoleDAO {

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultAdminRoleDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<AdminRole> findById(UUID id) {
    requireNonNull(id, "id cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity = session.find(AdminRoleEntity.class, id);
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public Optional<AdminRole> findByRoleName(String roleName) {
    requireNonNull(roleName, "roleName cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = "from AdminRoleEntity where roleName = :roleName";
      var entity =
          session
              .createQuery(hql, AdminRoleEntity.class)
              .setParameter("roleName", roleName)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public ImmutableList<AdminRole> list() {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = "from AdminRoleEntity order by roleName asc";
      var entities = session.createQuery(hql, AdminRoleEntity.class).list();
      return entities.stream().map(this::toRole).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public AdminRole create(String roleName) {
    requireNonNull(roleName, "roleName cannot be null");
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var sql = "INSERT INTO njall_admin.admin_roles (role_name) VALUES (:roleName) RETURNING id";
        var id =
            session
                .createNativeQuery(sql, UUID.class)
                .setParameter("roleName", roleName)
                .getSingleResult();
        tx.commit();
        return AdminRole.of(id, roleName);
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  private AdminRole toRole(AdminRoleEntity entity) {
    return AdminRole.of(entity.getId(), entity.getRoleName());
  }
}
