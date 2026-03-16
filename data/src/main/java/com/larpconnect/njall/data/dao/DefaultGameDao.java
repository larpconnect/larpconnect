package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Game;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultGameDao implements GameDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultGameDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Game> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Game.class, id));
  }

  @Override
  public Uni<Game> persist(Game entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
