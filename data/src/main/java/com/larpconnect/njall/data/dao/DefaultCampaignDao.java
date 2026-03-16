package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Campaign;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCampaignDao implements CampaignDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCampaignDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Campaign> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Campaign.class, id));
  }

  @Override
  public Uni<Campaign> persist(Campaign entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
