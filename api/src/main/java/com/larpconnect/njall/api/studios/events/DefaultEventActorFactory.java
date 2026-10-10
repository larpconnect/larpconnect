package com.larpconnect.njall.api.studios.events;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.EventDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultEventActorFactory implements EventActorFactory {

  private final EventDAO eventDao;

  @Inject
  DefaultEventActorFactory(EventDAO eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  public Behavior<EventCommand> create() {
    return Behaviors.setup(context -> new EventActor(context, eventDao));
  }
}
