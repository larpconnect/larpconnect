package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.ActorEndpoint;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultActorEndpointDao implements ActorEndpointDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultActorEndpointDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<ActorEndpoint> findById(ActorEndpoint.ActorEndpointId id) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.find(ActorEndpoint.class, id));
  }

  @Override
  public Uni<ActorEndpoint> persist(ActorEndpoint entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
