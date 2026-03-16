package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Studio;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultStudioDao.class)
public interface StudioDao {
  Uni<Studio> findById(UUID id);

  Uni<Studio> persist(Studio entity);
}
