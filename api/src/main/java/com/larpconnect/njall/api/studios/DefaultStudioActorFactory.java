package com.larpconnect.njall.api.studios;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.StudioDAO;
import com.larpconnect.njall.data.dao.StudioLookupDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultStudioActorFactory implements StudioActorFactory {

  private final StudioLookupDAO studioLookupDao;
  private final StudioDAO studioDao;

  @Inject
  DefaultStudioActorFactory(StudioLookupDAO studioLookupDao, StudioDAO studioDao) {
    this.studioLookupDao = studioLookupDao;
    this.studioDao = studioDao;
  }

  @Override
  public Behavior<StudioCommand> create() {
    return Behaviors.setup(context -> new StudioActor(context, studioLookupDao, studioDao));
  }
}
