package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.StudioRole;
import io.smallrye.mutiny.Uni;

@DefaultImplementation(DefaultStudioRoleDao.class)
public interface StudioRoleDao {
  Uni<StudioRole> findById(StudioRole.StudioRoleId id);

  Uni<StudioRole> persist(StudioRole entity);
}
