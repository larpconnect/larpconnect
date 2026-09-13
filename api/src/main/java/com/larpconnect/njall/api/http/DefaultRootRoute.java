package com.larpconnect.njall.api.http;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.larpconnect.njall.api.RouteProvider;
import java.util.Set;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

final class DefaultRootRoute extends AllDirectives implements RootRoute {

  private final Set<RouteProvider> routeProviders;

  @Inject
  DefaultRootRoute(Set<RouteProvider> routeProviders) {
    this.routeProviders =
        Set.copyOf(requireNonNull(routeProviders, "routeProviders must not be null"));
  }

  @Override
  public Route route() {
    var composite = buildRootEndpoint();
    for (var provider : routeProviders) {
      composite = concat(composite, provider.route());
    }
    return composite;
  }

  private Route buildRootEndpoint() {
    return pathEndOrSingleSlash(() -> get(() -> complete(StatusCodes.OK, "")));
  }
}
