package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.LarpSystem;
import java.util.UUID;

@DefaultImplementation(DefaultLarpSystemDao.class)
public interface LarpSystemDao extends Dao<LarpSystem, UUID> {}
