package com.larpconnect.njall.api;

import org.apache.pekko.http.javadsl.server.Route;

/** Contributes an HTTP {@link Route} to the application router. */
@FunctionalInterface
public interface RouteProvider {

  /**
   * Supplies the configured HTTP route.
   *
   * @return The Pekko HTTP route.
   */
  Route route();
}
