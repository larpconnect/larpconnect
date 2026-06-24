package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for the main Server module configuration. */
public final class ServerModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new ServerModule());
    assertThat(injector).isNotNull();

    ServerService service1 = injector.getInstance(ServerService.class);
    ServerService service2 = injector.getInstance(ServerService.class);
    assertThat(service1).isNotNull();
    assertThat(service1).isSameAs(service2);
  }
}
