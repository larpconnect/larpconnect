package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Collection;
import java.util.UUID;

@DefaultImplementation(DefaultCollectionDao.class)
public interface CollectionDao extends Dao<Collection, UUID> {}
