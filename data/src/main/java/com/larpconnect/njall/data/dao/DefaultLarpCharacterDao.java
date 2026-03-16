package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.LarpCharacter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultLarpCharacterDao implements LarpCharacterDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultLarpCharacterDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<LarpCharacter> findById(UUID id) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.find(LarpCharacter.class, id));
  }

  @Override
  public Uni<LarpCharacter> persist(LarpCharacter entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
