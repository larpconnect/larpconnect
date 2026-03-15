package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Actor;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultActorDao.class)
public interface ActorDao {
  Uni<Actor> findById(UUID id);

  Uni<Actor> persist(Actor actor);
}
