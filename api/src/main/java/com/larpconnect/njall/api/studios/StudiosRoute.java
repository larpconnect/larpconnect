package com.larpconnect.njall.api.studios;

import com.larpconnect.njall.api.http.RouteProvider;
import org.apache.pekko.http.javadsl.server.Route;

/** Defines the HTTP routing contract for user-space tenanted studio retrieval. */
public interface StudiosRoute extends RouteProvider {

  /**
   * Generates the studio {@link Route}, including aggregated subordinate studio routes.
   *
   * @return The configured Pekko HTTP route.
   */
  @Override
  Route route();
}
