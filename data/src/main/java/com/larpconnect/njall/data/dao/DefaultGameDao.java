package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Game;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultGameDao extends AbstractDao<Game, UUID> implements GameDao {

  @Inject
  DefaultGameDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Game.class);
  }
}
