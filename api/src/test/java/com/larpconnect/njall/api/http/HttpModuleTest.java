package com.larpconnect.njall.api.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.larpconnect.njall.api.admin.AdminRoute;
import org.apache.pekko.http.javadsl.server.Directives;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HttpModuleTest {

  @Test
  @DisplayName("configure binds RootRoute to DefaultRootRoute")
  void configure_createsInjector_bindsRootRoute() {
    var stubAdminModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(AdminRoute.class).toInstance(Directives::reject);
          }
        };

    var injector = Guice.createInjector(new HttpModule(), stubAdminModule);
    var rootRoute = injector.getInstance(RootRoute.class);

    assertThat(rootRoute).isInstanceOf(DefaultRootRoute.class);
  }
}
