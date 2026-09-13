package com.larpconnect.njall.api.http;

import com.google.inject.Inject;
import com.larpconnect.njall.api.admin.AdminRoute;
import java.util.Objects;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

final class DefaultRootRoute extends AllDirectives implements RootRoute {

  private final AdminRoute adminRoute;

  @Inject
  DefaultRootRoute(AdminRoute adminRoute) {
    this.adminRoute = Objects.requireNonNull(adminRoute, "adminRoute must not be null");
  }

  @Override
  public Route route() {
    return concat(buildRootEndpoint(), adminRoute.route());
  }

  private Route buildRootEndpoint() {
    return pathEndOrSingleSlash(() -> get(() -> complete(StatusCodes.OK, "")));
  }
}
