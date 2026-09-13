package com.larpconnect.njall.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.larpconnect.njall.api.http.RootRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ApiModuleTest {

  @Test
  @DisplayName("configure installs HttpModule and binds RootRoute")
  void configure_createsInjector_bindsRootRoute() {
    var injector = Guice.createInjector(new ApiModule());
    var rootRoute = injector.getInstance(RootRoute.class);

    assertThat(rootRoute).isNotNull();
  }
}
