package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Entity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

public final class DefaultEntityDao implements EntityDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  public DefaultEntityDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Entity> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Entity.class, id));
  }

  @Override
  public Uni<Entity> persist(Entity entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
