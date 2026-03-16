package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Individual;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultIndividualDao implements IndividualDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultIndividualDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<Individual> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(Individual.class, id));
  }

  @Override
  public Uni<Individual> persist(Individual entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
