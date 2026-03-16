package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Role;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultRoleDao implements RoleDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultRoleDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Role> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Role.class, id));
  }

  @Override
  public Uni<Role> persist(Role entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
