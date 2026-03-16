package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ActorEndpoint;
import io.smallrye.mutiny.Uni;

@DefaultImplementation(DefaultActorEndpointDao.class)
public interface ActorEndpointDao {
  Uni<ActorEndpoint> findById(ActorEndpoint.ActorEndpointId id);

  Uni<ActorEndpoint> persist(ActorEndpoint entity);
}
