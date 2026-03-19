package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.System;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultSystemDao extends AbstractDao<System, UUID> implements SystemDao {

  @Inject
  DefaultSystemDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, System.class);
  }
}
