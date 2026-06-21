package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for the main Server module configuration and application entry point. */
public final class ServerModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new ServerModule());
    assertThat(injector).isNotNull();
  }

  @Test
  public void main_withServerApp_succeeds() {
    // Invoke main to ensure the application starts up and is fully covered
    ServerApp.main(new String[0]);
  }
}
