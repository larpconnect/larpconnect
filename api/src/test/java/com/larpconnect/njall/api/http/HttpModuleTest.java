package com.larpconnect.njall.api.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.larpconnect.njall.common.telemetry.TelemetryModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HttpModuleTest {

  @Test
  @DisplayName("configure binds RootRoute to DefaultRootRoute and TracingDirective")
  void configure_createsInjector_bindsRootRoute() {
    var injector = Guice.createInjector(new HttpModule(), new TelemetryModule());
    var rootRoute = injector.getInstance(RootRoute.class);
    var tracingDirective = injector.getInstance(TracingDirective.class);

    assertThat(rootRoute).isInstanceOf(DefaultRootRoute.class);
    assertThat(tracingDirective).isInstanceOf(DefaultTracingDirective.class);
  }
}
