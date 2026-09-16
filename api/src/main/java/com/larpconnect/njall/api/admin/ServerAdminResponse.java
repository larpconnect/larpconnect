package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.Server;

/** Response protocol emitted by the server admin actor. */
public sealed interface ServerAdminResponse {

  /**
   * Indicates servers were successfully retrieved.
   *
   * @param servers Immutable list of retrieved servers.
   */
  record ServerList(ImmutableList<Server> servers) implements ServerAdminResponse {}

  /**
   * Indicates failure during server retrieval.
   *
   * @param reason Diagnostic explanation of the error.
   */
  record Failure(String reason) implements ServerAdminResponse {}

  /**
   * Pure factory method constructing a server list response.
   *
   * @param servers Immutable list of servers.
   * @return A {@link ServerList} response instance.
   */
  static ServerAdminResponse success(ImmutableList<Server> servers) {
    return new ServerList(servers);
  }

  /**
   * Pure factory method constructing a failure response.
   *
   * @param reason The error explanation.
   * @return A {@link Failure} response instance.
   */
  static ServerAdminResponse failure(String reason) {
    return new Failure(reason);
  }
}
