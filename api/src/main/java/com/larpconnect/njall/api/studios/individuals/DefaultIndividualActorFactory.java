package com.larpconnect.njall.api.studios.individuals;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultIndividualActorFactory implements IndividualActorFactory {

  private final IndividualDAO individualDao;

  @Inject
  DefaultIndividualActorFactory(IndividualDAO individualDao) {
    this.individualDao = individualDao;
  }

  @Override
  public Behavior<IndividualCommand> create() {
    return Behaviors.setup(context -> new IndividualActor(context, individualDao));
  }
}
