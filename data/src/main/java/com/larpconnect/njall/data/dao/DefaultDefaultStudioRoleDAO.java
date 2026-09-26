package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.DefaultStudioRole;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultDefaultStudioRoleDAO implements DefaultStudioRoleDAO {

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultDefaultStudioRoleDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
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
      var hql = "from DefaultStudioRoleEntity where name = :name";
      var entity =
          session
              .createQuery(hql, DefaultStudioRoleEntity.class)
              .setParameter("name", name)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toRole);
    }
  }

  @Override
  public ImmutableList<DefaultStudioRole> list() {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = "from DefaultStudioRoleEntity order by name asc";
      var entities = session.createQuery(hql, DefaultStudioRoleEntity.class).list();
      return entities.stream().map(this::toRole).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public DefaultStudioRole create(String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var sql = "INSERT INTO njall_users.default_studio_roles (name) VALUES (:name) RETURNING id";
        var id =
            session.createNativeQuery(sql, UUID.class).setParameter("name", name).getSingleResult();
        tx.commit();
        return new DefaultStudioRole(id, name);
      } catch (Exception e) {
        tx.rollback();
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
        tx.rollback();
        throw e;
      }
    }
  }

  private DefaultStudioRole toRole(DefaultStudioRoleEntity entity) {
    return new DefaultStudioRole(entity.getId(), entity.getName());
  }
}
