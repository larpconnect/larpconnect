package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Actor;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

public final class DefaultActorDao implements ActorDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  public DefaultActorDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Actor> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Actor.class, id));
  }

  @Override
  public Uni<Actor> persist(Actor actor) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(actor).chain(session::flush).replaceWith(actor));
  }
}
