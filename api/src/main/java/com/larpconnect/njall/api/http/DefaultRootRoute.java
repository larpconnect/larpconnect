package com.larpconnect.njall.api.http;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import java.util.Set;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

final class DefaultRootRoute extends AllDirectives implements RootRoute {

  private final Set<RouteProvider> routeProviders;
  private final TracingDirective tracingDirective;

  @Inject
  DefaultRootRoute(Set<RouteProvider> routeProviders, TracingDirective tracingDirective) {
    this.routeProviders = Set.copyOf(routeProviders);
    this.tracingDirective = requireNonNull(tracingDirective, "tracingDirective cannot be null");
  }

  @Override
  public Route route() {
    return tracingDirective.trace(this::buildCompositeRoute);
  }

  private Route buildCompositeRoute() {
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
