package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.StudioRole;
import com.larpconnect.njall.common.annotations.DefaultImplementation;
import io.smallrye.mutiny.Uni;

/** DAO for StudioRole. */
@com.larpconnect.njall.common.annotations.DefaultImplementation(DefaultStudioRoleDao.class)
@DefaultImplementation(DefaultStudioRoleDao.class)
public interface StudioRoleDao {
  Uni<StudioRole> findById(StudioRole.StudioRoleId id);

  Uni<Void> persist(StudioRole entity);
}
