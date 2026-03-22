package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.data.entity.Individual;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

@BuildWith(DaoModule.class)
final class DefaultIndividualDao extends AbstractDao<Individual, UUID> implements IndividualDao {

  @Inject
  DefaultIndividualDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Individual.class);
  }
}
