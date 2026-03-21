package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.DatabaseObject;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Provider;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Base implementation of the Dao interface.
 *
 * @param <T> The entity type
 * @param <ID> The type of the identifier
 */
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
  public Uni<T> findById(ID id) {
    return getSessionFactory().withSession(session -> session.find(entityClass, id));
  }

  @Override
  public Uni<Void> persist(T entity) {
    return getSessionFactory().withSession(session -> session.persist(entity).call(session::flush));
  }
}
