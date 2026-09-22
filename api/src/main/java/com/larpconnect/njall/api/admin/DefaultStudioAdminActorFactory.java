package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.StudioDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Default implementation of {@link StudioAdminActorFactory} creating {@link StudioAdminActor}. */
final class DefaultStudioAdminActorFactory implements StudioAdminActorFactory {

  private final StudioDAO studioDao;

  @Inject
  DefaultStudioAdminActorFactory(StudioDAO studioDao) {
    this.studioDao = studioDao;
  }

  @Override
  public Behavior<StudioAdminCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private StudioAdminActor createActor(ActorContext<StudioAdminCommand> context) {
    return new StudioAdminActor(context, studioDao);
  }
}
