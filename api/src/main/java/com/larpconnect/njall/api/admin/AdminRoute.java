package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.api.http.RouteProvider;
import org.apache.pekko.http.javadsl.server.Route;

/** Defines the administrative HTTP routing contract for Project Njall. */
public interface AdminRoute extends RouteProvider {

  /**
   * Generates the administrative {@link Route}.
   *
   * @return The configured Pekko HTTP route.
   */
  @Override
  Route route();
}
