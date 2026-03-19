package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Collection;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCollectionDao extends AbstractDao<Collection, UUID> implements CollectionDao {

  @Inject
  DefaultCollectionDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Collection.class);
  }
}
