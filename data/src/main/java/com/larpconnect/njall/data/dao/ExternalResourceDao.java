package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ExternalResource;
import java.util.UUID;

/** DAO for ExternalResource. */
@DefaultImplementation(DefaultExternalResourceDao.class)
public interface ExternalResourceDao extends Dao<ExternalResource, UUID> {}
