package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.data.entity.ActorEndpoint;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import javax.annotation.Nullable;
import org.hibernate.reactive.mutiny.Mutiny;

@BuildWith(DaoModule.class)
final class DefaultActorEndpointDao implements ActorEndpointDao {

  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultActorEndpointDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<ActorEndpoint> findById(@Nullable String serverId, ActorEndpoint.ActorEndpointId id) {
    return sessionFactoryProvider
        .get()
        .withSession(
            TenantContext.formatTenantId(serverId),
            session -> session.find(ActorEndpoint.class, id));
  }

  @Override
  public Uni<Void> persist(@Nullable String serverId, ActorEndpoint entity) {
    return sessionFactoryProvider
        .get()
        .withSession(
            TenantContext.formatTenantId(serverId),
            session -> session.persist(entity).call(session::flush));
  }
}
