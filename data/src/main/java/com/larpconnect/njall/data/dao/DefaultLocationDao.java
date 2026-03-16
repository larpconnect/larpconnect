package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Location;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultLocationDao implements LocationDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultLocationDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Location> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Location.class, id));
  }

  @Override
  public Uni<Location> persist(Location entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
