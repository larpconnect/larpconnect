package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.ServerDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Default implementation of {@link ServerAdminActorFactory} creating {@link ServerAdminActor}. */
final class DefaultServerAdminActorFactory implements ServerAdminActorFactory {

  private final ServerDAO serverDao;

  @Inject
  DefaultServerAdminActorFactory(ServerDAO serverDao) {
    this.serverDao = serverDao;
  }

  @Override
  public Behavior<ServerAdminCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private ServerAdminActor createActor(ActorContext<ServerAdminCommand> context) {
    return new ServerAdminActor(context, serverDao);
  }
}
