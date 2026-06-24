package org.larpconnect.data.dao;

import org.larpconnect.data.entity.Game;

/** DAO interface for managing Game entities. */
public sealed interface GameDao extends BaseDao<Game, GameDao> permits DefaultGameDao {}
