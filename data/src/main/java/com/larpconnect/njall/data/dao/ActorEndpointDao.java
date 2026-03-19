package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.ActorEndpoint;
import io.smallrye.mutiny.Uni;

/** DAO for ActorEndpoint. */
@com.larpconnect.njall.common.annotations.DefaultImplementation(DefaultActorEndpointDao.class)
public interface ActorEndpointDao {

  Uni<ActorEndpoint> findById(ActorEndpoint.ActorEndpointId id);

  Uni<Void> persist(ActorEndpoint entity);
}
