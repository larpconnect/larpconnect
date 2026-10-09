package com.larpconnect.njall.data.dao.admin;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.DefaultStudioRole;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultStudioRoleDAO implements StudioRoleDAO {

  private static final String HQL_FIND_BY_NAME = "from DefaultStudioRoleEntity where name = :name";
  private static final String HQL_LIST = "from DefaultStudioRoleEntity order by name asc";
  private static final String SQL_INSERT_ROLE =
      "INSERT INTO njall_users.default_studio_roles (name) VALUES (:name) RETURNING id";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultStudioRoleDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<DefaultStudioRole> findById(UUID id) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity = session.find(DefaultStudioRoleEntity.class, id);
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public Optional<DefaultStudioRole> findByName(String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entity =
          session
              .createQuery(HQL_FIND_BY_NAME, DefaultStudioRoleEntity.class)
              .setParameter("name", name)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public ImmutableList<DefaultStudioRole> list() {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var entities = session.createQuery(HQL_LIST, DefaultStudioRoleEntity.class).list();
      return entities.stream().map(this::toRole).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public DefaultStudioRole create(String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var id =
            session
                .createNativeQuery(SQL_INSERT_ROLE, UUID.class)
                .setParameter("name", name)
                .getSingleResult();
        tx.commit();
        return new DefaultStudioRole(id, name);
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
  public Optional<DefaultStudioRole> update(UUID id, String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var entity = session.find(DefaultStudioRoleEntity.class, id);
        if (entity == null) {
          tx.rollback();
          return Optional.empty();
        }
        entity.setName(name);
        session.merge(entity);
        tx.commit();
        return Optional.of(toRole(entity));
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

  private DefaultStudioRole toRole(DefaultStudioRoleEntity entity) {
    return new DefaultStudioRole(entity.getId(), entity.getName());
  }
}
