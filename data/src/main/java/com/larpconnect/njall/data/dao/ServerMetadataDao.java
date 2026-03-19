package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ServerMetadata;
import java.util.UUID;

/** DAO for ServerMetadata. */
@DefaultImplementation(DefaultServerMetadataDao.class)
public interface ServerMetadataDao extends Dao<ServerMetadata, UUID> {}
