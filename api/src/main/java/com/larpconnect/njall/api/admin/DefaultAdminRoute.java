package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import com.larpconnect.njall.api.admin.health.HealthAdminRoute;
import com.larpconnect.njall.api.admin.roles.RoleAdminRoute;
import com.larpconnect.njall.api.admin.servers.ServersAdminRoute;
import com.larpconnect.njall.api.admin.studioroles.StudioRoleAdminRoute;
import com.larpconnect.njall.api.admin.studios.StudioAdminRoute;
import com.larpconnect.njall.api.admin.users.UserAdminRoute;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

final class DefaultAdminRoute extends AllDirectives implements AdminRoute {

  private final HealthAdminRoute healthAdminRoute;
  private final ServersAdminRoute serversAdminRoute;
  private final StudioAdminRoute studioAdminRoute;
  private final StudioRoleAdminRoute studioRoleAdminRoute;
  private final UserAdminRoute userAdminRoute;
  private final RoleAdminRoute roleAdminRoute;

  @Inject
  DefaultAdminRoute(
      HealthAdminRoute healthAdminRoute,
      ServersAdminRoute serversAdminRoute,
      StudioAdminRoute studioAdminRoute,
      StudioRoleAdminRoute studioRoleAdminRoute,
      UserAdminRoute userAdminRoute,
      RoleAdminRoute roleAdminRoute) {
    this.healthAdminRoute = healthAdminRoute;
    this.serversAdminRoute = serversAdminRoute;
    this.studioAdminRoute = studioAdminRoute;
    this.studioRoleAdminRoute = studioRoleAdminRoute;
    this.userAdminRoute = userAdminRoute;
    this.roleAdminRoute = roleAdminRoute;
  }

  @Override
  public Route route() {
    return concat(
        healthAdminRoute.route(),
        serversAdminRoute.route(),
        studioAdminRoute.route(),
        studioRoleAdminRoute.route(),
        userAdminRoute.route(),
        roleAdminRoute.route());
  }
}
