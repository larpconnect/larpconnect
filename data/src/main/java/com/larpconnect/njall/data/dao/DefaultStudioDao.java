package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Studio;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultStudioDao extends AbstractDao<Studio, UUID> implements StudioDao {

  @Inject
  DefaultStudioDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Studio.class);
  }
}
