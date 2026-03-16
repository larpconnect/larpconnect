package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.CollectionItem;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultCollectionItemDao.class)
public interface CollectionItemDao {
  Uni<CollectionItem> findById(UUID id);

  Uni<CollectionItem> persist(CollectionItem entity);
}
