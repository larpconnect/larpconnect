package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Collection;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultCollectionDao.class)
public interface CollectionDao {
  Uni<Collection> findById(UUID id);

  Uni<Collection> persist(Collection entity);
}
