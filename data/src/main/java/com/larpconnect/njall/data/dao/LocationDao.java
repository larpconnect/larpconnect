package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Location;
import java.util.UUID;

@DefaultImplementation(DefaultLocationDao.class)
public interface LocationDao extends Dao<Location, UUID> {}
