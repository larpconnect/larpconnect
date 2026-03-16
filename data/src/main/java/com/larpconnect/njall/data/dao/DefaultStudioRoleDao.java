package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.StudioRole;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultStudioRoleDao implements StudioRoleDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultStudioRoleDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<StudioRole> findById(StudioRole.StudioRoleId id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(StudioRole.class, id));
  }

  @Override
  public Uni<StudioRole> persist(StudioRole entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
