package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.CollectionItem;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCollectionItemDao extends AbstractDao<CollectionItem, UUID>
    implements CollectionItemDao {

  @Inject
  DefaultCollectionItemDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, CollectionItem.class);
  }
}
