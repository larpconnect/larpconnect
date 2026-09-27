package com.larpconnect.njall.api.studios;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.StudioDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultStudioActorFactory implements StudioActorFactory {

  private final StudioDAO studioDao;

  @Inject
  DefaultStudioActorFactory(StudioDAO studioDao) {
    this.studioDao = studioDao;
  }

  @Override
  public Behavior<StudioCommand> create() {
    return Behaviors.setup(context -> new StudioActor(context, studioDao));
  }
}
