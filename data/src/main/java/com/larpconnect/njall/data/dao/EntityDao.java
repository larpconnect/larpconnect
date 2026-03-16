package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Entity;
import java.util.UUID;

@DefaultImplementation(DefaultEntityDao.class)
public interface EntityDao extends Dao<Entity, UUID> {}
