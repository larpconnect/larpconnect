package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.ServerDAO;
import com.larpconnect.njall.data.domain.Server;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object-oriented Apache Pekko Typed actor executing server administration operations.
 *
 * <p>This actor is spawned on a dedicated blocking dispatcher ({@code
 * larpconnect.blocking-dispatcher}) to isolate synchronous database interactions from the default
 * actor system thread pool.
 */
public final class ServerAdminActor extends AbstractBehavior<ServerAdminCommand> {

  private final Logger logger = LoggerFactory.getLogger(ServerAdminActor.class);
  private final ServerDAO serverDao;

  public ServerAdminActor(ActorContext<ServerAdminCommand> context, ServerDAO serverDao) {
    super(context);
    this.serverDao = serverDao;
  }

  @Override
  public Receive<ServerAdminCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(ServerAdminCommand.ListServers.class, this::onListServers)
        .build();
  }

  private Behavior<ServerAdminCommand> onListServers(ServerAdminCommand.ListServers cmd) {
    try {
      ImmutableList<Server> servers = serverDao.list();
      cmd.replyTo().tell(ServerAdminResponse.success(servers));
    } catch (Exception e) {
      handleError(cmd.replyTo(), e);
    }
    return this;
  }

  private void handleError(ActorRef<ServerAdminResponse> replyTo, Exception error) {
    logger.error("Failed to query servers via ServerDAO", error);
    var message = error.getMessage();
    var reason = message != null && !message.isBlank() ? message : "Error querying servers";
    replyTo.tell(ServerAdminResponse.failure(reason));
  }
}
