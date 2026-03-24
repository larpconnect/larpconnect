package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.DatabaseObject;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Provider;
import javax.annotation.Nullable;
import org.hibernate.reactive.mutiny.Mutiny;

abstract class AbstractDao<T extends DatabaseObject, ID> implements Dao<T, ID> {

  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;
  private final Class<T> entityClass;

  protected AbstractDao(
      Provider<Mutiny.SessionFactory> sessionFactoryProvider, Class<T> entityClass) {
    this.sessionFactoryProvider = sessionFactoryProvider;
    this.entityClass = entityClass;
  }

  protected Mutiny.SessionFactory getSessionFactory() {
    return sessionFactoryProvider.get();
  }

  @Override
  public Uni<T> findById(@Nullable String serverId, ID id) {
    return getSessionFactory()
        .withSession(
            TenantContext.formatTenantId(serverId), session -> session.find(entityClass, id));
  }

  @Override
  public Uni<Void> persist(@Nullable String serverId, T entity) {
    return getSessionFactory()
        .withSession(
            TenantContext.formatTenantId(serverId),
            session -> session.persist(entity).call(session::flush));
  }
}
