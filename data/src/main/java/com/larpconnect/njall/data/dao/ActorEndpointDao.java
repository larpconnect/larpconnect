package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ActorEndpoint;
import io.smallrye.mutiny.Uni;
import javax.annotation.Nullable;

/** DAO for ActorEndpoint. */
@DefaultImplementation(DefaultActorEndpointDao.class)
public interface ActorEndpointDao {

  Uni<ActorEndpoint> findById(@Nullable String serverId, ActorEndpoint.ActorEndpointId id);

  Uni<Void> persist(@Nullable String serverId, ActorEndpoint entity);
}
