package com.larpconnect.njall.api.admin;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.admin.common.AdminCommonModule;
import com.larpconnect.njall.api.admin.health.HealthAdminModule;
import com.larpconnect.njall.api.admin.roles.RolesAdminModule;
import com.larpconnect.njall.api.admin.servers.ServersAdminModule;
import com.larpconnect.njall.api.admin.studioroles.StudioRolesAdminModule;
import com.larpconnect.njall.api.admin.studios.StudiosAdminModule;
import com.larpconnect.njall.api.admin.users.UsersAdminModule;
import com.larpconnect.njall.api.http.RouteProvider;

/** Guice module configuring administrative routes and installing all admin submodules. */
public final class AdminModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new AdminCommonModule());
    install(new HealthAdminModule());
    install(new ServersAdminModule());
    install(new StudiosAdminModule());
    install(new StudioRolesAdminModule());
    install(new UsersAdminModule());
    install(new RolesAdminModule());

    bind(AdminRoute.class).to(DefaultAdminRoute.class);
    Multibinder.newSetBinder(binder(), RouteProvider.class)
        .addBinding()
        .to(DefaultAdminRoute.class);
  }
}
