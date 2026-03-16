package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.CharacterInstance;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCharacterInstanceDao implements CharacterInstanceDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCharacterInstanceDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<CharacterInstance> findById(UUID id) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.find(CharacterInstance.class, id));
  }

  @Override
  public Uni<CharacterInstance> persist(CharacterInstance entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
