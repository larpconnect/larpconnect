package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.System;
import java.util.UUID;

/** DAO for System. */
@DefaultImplementation(DefaultSystemDao.class)
public interface SystemDao extends Dao<System, UUID> {}
