package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ServerMetadata;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultServerMetadataDao.class)
public interface ServerMetadataDao {
  Uni<ServerMetadata> findById(UUID id);

  Uni<ServerMetadata> persist(ServerMetadata entity);
}
