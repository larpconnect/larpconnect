package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Entity;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultEntityDao.class)
public interface EntityDao {
  Uni<Entity> findById(UUID id);

  Uni<Entity> persist(Entity entity);
}
