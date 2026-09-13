package com.larpconnect.njall.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.typesafe.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ConfigModuleTest {

  @Test
  @DisplayName("provideConfig provides loaded reference configuration")
  void provideConfig_createsInjector_providesLoadedConfig() {
    var injector = Guice.createInjector(new ConfigModule());
    var config = injector.getInstance(Config.class);

    assertThat(config.getString("larpconnect.server.host")).isEqualTo("0.0.0.0");
  }

  @Test
  @DisplayName("provideServerConfig provides ServerConfig with default port 8080")
  void provideServerConfig_createsInjector_providesDefaultServerConfig() {
    var injector = Guice.createInjector(new ConfigModule());
    var serverConfig = injector.getInstance(ServerConfig.class);

    assertThat(serverConfig.port()).isEqualTo(8080);
  }
}
