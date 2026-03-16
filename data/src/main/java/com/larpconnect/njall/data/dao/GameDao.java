package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Game;
import java.util.UUID;

@DefaultImplementation(DefaultGameDao.class)
public interface GameDao extends Dao<Game, UUID> {}
