package com.larpconnect.njall.api.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HttpModuleTest {

  @Test
  @DisplayName("configure binds RootRoute to DefaultRootRoute")
  void configure_createsInjector_bindsRootRoute() {
    var injector = Guice.createInjector(new HttpModule());
    var rootRoute = injector.getInstance(RootRoute.class);

    assertThat(rootRoute).isInstanceOf(DefaultRootRoute.class);
  }
}
