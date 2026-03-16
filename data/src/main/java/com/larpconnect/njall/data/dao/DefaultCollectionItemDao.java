package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.CollectionItem;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCollectionItemDao implements CollectionItemDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCollectionItemDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<CollectionItem> findById(UUID id) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.find(CollectionItem.class, id));
  }

  @Override
  public Uni<CollectionItem> persist(CollectionItem entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
