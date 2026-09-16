package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for querying the server administration actor. */
public sealed interface ServerAdminCommand {

  /**
   * Command instructing the server admin actor to retrieve all registered servers.
   *
   * @param replyTo The actor reference to reply to with a {@link ServerAdminResponse}.
   */
  record ListServers(ActorRef<ServerAdminResponse> replyTo) implements ServerAdminCommand {}
}
