package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Studio;
import java.util.UUID;

@DefaultImplementation(DefaultStudioDao.class)
public interface StudioDao extends Dao<Studio, UUID> {}
