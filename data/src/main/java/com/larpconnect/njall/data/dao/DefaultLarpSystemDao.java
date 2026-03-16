package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.LarpSystem;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultLarpSystemDao implements LarpSystemDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultLarpSystemDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<LarpSystem> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(LarpSystem.class, id));
  }

  @Override
  public Uni<LarpSystem> persist(LarpSystem entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
