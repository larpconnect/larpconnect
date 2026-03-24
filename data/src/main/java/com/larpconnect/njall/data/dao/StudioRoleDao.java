package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.StudioRole;
import io.smallrye.mutiny.Uni;
import javax.annotation.Nullable;

/** DAO for StudioRole. */
@DefaultImplementation(DefaultStudioRoleDao.class)
public interface StudioRoleDao {
  Uni<StudioRole> findById(@Nullable String serverId, StudioRole.StudioRoleId id);

  Uni<Void> persist(@Nullable String serverId, StudioRole entity);
}
