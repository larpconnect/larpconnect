package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ActorEndpoint;

@DefaultImplementation(DefaultActorEndpointDao.class)
public interface ActorEndpointDao extends Dao<ActorEndpoint, ActorEndpoint.ActorEndpointId> {}
