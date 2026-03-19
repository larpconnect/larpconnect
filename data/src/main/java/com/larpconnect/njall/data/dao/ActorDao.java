package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Actor;
import java.util.UUID;

/** DAO for Actor. */
@DefaultImplementation(DefaultActorDao.class)
public interface ActorDao extends Dao<Actor, UUID> {}
