package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Location;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultLocationDao.class)
public interface LocationDao {
  Uni<Location> findById(UUID id);

  Uni<Location> persist(Location entity);
}
