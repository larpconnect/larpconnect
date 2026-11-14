package com.larpconnect.njall.api.studios;

import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import java.util.Comparator;
import java.util.Set;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.Route;

/** Subordinate route aggregator nested under user-space studio endpoints. */
final class StudioSubRoutes {

  private final Route compositeRoute;

  @Inject
  StudioSubRoutes(@StudioSubRoute Set<RouteProvider> routeProviders) {
    this(composeRoutes(routeProviders));
  }

  StudioSubRoutes(Route compositeRoute) {
    this.compositeRoute = compositeRoute;
  }

  private static Route composeRoutes(Set<RouteProvider> routeProviders) {
    return routeProviders.stream()
        .sorted(Comparator.comparing(provider -> provider.getClass().getName()))
        .map(RouteProvider::route)
        .reduce((first, second) -> Directives.concat(first, second))
        .orElseGet(Directives::reject);
  }

  /**
   * Concatenates all subordinate studio routes.
   *
   * @return Composite Pekko HTTP route.
   */
  Route route() {
    return compositeRoute;
  }
}
