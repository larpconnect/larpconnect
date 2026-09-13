package com.larpconnect.njall.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.larpconnect.njall.common.config.ServerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CommonModuleTest {

  @Test
  @DisplayName("configure installs submodules and resolves ServerConfig")
  void configure_createsInjector_resolvesServerConfig() {
    var injector = Guice.createInjector(new CommonModule());
    var serverConfig = injector.getInstance(ServerConfig.class);

    assertThat(serverConfig.host()).isEqualTo("0.0.0.0");
    assertThat(serverConfig.port()).isEqualTo(8080);
  }
}
