package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ActorEndpoint;
import io.smallrye.mutiny.Uni;

/** DAO for ActorEndpoint. */
@DefaultImplementation(DefaultActorEndpointDao.class)
public interface ActorEndpointDao {

  Uni<ActorEndpoint> findById(String serverId, ActorEndpoint.ActorEndpointId id);

  Uni<Void> persist(String serverId, ActorEndpoint entity);
}
