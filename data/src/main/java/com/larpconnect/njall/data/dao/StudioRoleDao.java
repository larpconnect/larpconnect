package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.StudioRole;

@DefaultImplementation(DefaultStudioRoleDao.class)
public interface StudioRoleDao extends Dao<StudioRole, StudioRole.StudioRoleId> {}
