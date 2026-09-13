package com.larpconnect.njall.api.admin;

import org.apache.pekko.http.javadsl.server.Route;

/** Defines the administrative HTTP routing contract for Project Njall. */
public interface AdminRoute {

  /**
   * Generates the administrative {@link Route}.
   *
   * @return The configured Pekko HTTP route.
   */
  Route route();
}
