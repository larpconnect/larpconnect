package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Collection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCollectionDao implements CollectionDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCollectionDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Collection> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Collection.class, id));
  }

  @Override
  public Uni<Collection> persist(Collection entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
