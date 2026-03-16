package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.CollectionItem;
import java.util.UUID;

@DefaultImplementation(DefaultCollectionItemDao.class)
public interface CollectionItemDao extends Dao<CollectionItem, UUID> {}
