package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Location;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultLocationDao extends AbstractDao<Location, UUID> implements LocationDao {

  @Inject
  DefaultLocationDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Location.class);
  }
}
