package com.larpconnect.njall.api.studios.games;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import org.apache.pekko.actor.typed.Behavior;

final class DefaultGameActorFactory implements GameActorFactory {

  private final GameDAO gameDao;

  @Inject
  DefaultGameActorFactory(GameDAO gameDao) {
    this.gameDao = gameDao;
  }

  @Override
  public Behavior<GameCommand> create() {
    return GameActor.create(gameDao);
  }
}
