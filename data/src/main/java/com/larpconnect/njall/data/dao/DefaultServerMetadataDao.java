package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.ServerMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultServerMetadataDao extends AbstractDao<ServerMetadata, UUID>
    implements ServerMetadataDao {

  @Inject
  DefaultServerMetadataDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, ServerMetadata.class);
  }
}
