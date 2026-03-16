package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.ServerMetadata;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultServerMetadataDao implements ServerMetadataDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultServerMetadataDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<ServerMetadata> findById(UUID id) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.find(ServerMetadata.class, id));
  }

  @Override
  public Uni<ServerMetadata> persist(ServerMetadata entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
