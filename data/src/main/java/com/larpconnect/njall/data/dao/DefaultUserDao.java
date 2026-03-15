package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.User;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

public final class DefaultUserDao implements UserDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultUserDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<User> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(User.class, id));
  }

  @Override
  public Uni<User> persist(User user) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(user).chain(session::flush).replaceWith(user));
  }
}
