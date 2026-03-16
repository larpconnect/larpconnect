package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Studio;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultStudioDao implements StudioDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultStudioDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Studio> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Studio.class, id));
  }

  @Override
  public Uni<Studio> persist(Studio entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
