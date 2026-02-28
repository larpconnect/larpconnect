package com.larpconnect.njall.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.larpconnect.njall.common.time.TimeService;
import org.junit.jupiter.api.Test;

class CommonModuleTest {

  @Test
  void module_bindsTimeService_successfully() {
    Injector injector = Guice.createInjector(new CommonModule());
    TimeService timeService = injector.getInstance(TimeService.class);

    assertThat(timeService).isNotNull();
    // It should be bound as a Singleton (using MonotonicTimeServiceProvider which creates new, but
    // Guice should cache it if we use in(Singleton.class), wait! The provider is a Singleton, but
    // does that make the provided value a singleton? No, we should use .in(Singleton.class) on the
    // binding!)
    TimeService secondInstance = injector.getInstance(TimeService.class);
    assertThat(timeService).isSameAs(secondInstance);
  }
}
