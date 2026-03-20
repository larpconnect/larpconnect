package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.LarpSystem;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultLarpSystemDao extends AbstractDao<LarpSystem, UUID> implements LarpSystemDao {

  @Inject
  DefaultLarpSystemDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, LarpSystem.class);
  }
}
