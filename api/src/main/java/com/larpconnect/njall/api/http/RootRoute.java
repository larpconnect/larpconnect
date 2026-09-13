package com.larpconnect.njall.api.http;

import org.apache.pekko.http.javadsl.server.Route;

/** Defines the root HTTP routing contract for Project Njall. */
public interface RootRoute {

  /**
   * Generates the root {@link Route}.
   *
   * @return The configured Pekko HTTP route.
   */
  Route route();
}
