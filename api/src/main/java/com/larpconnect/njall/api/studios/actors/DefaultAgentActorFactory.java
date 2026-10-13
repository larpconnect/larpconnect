package com.larpconnect.njall.api.studios.actors;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultAgentActorFactory implements AgentActorFactory {

  private final AgentDAO agentDao;
  private final CollectionDAO collectionDao;

  @Inject
  DefaultAgentActorFactory(AgentDAO agentDao, CollectionDAO collectionDao) {
    this.agentDao = agentDao;
    this.collectionDao = collectionDao;
  }

  @Override
  public Behavior<AgentCommand> create() {
    return Behaviors.setup(context -> new AgentActor(context, agentDao, collectionDao));
  }
}
