package com.larpconnect.njall.api.studios.systems;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import org.apache.pekko.actor.typed.Behavior;

final class DefaultGameSystemActorFactory implements GameSystemActorFactory {

  private final GameSystemDAO gameSystemDao;

  @Inject
  DefaultGameSystemActorFactory(GameSystemDAO gameSystemDao) {
    this.gameSystemDao = gameSystemDao;
  }

  @Override
  public Behavior<GameSystemCommand> create() {
    return GameSystemActor.create(gameSystemDao);
  }
}
