package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Game;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultGameDao.class)
public interface GameDao {
  Uni<Game> findById(UUID id);

  Uni<Game> persist(Game entity);
}
