package com.larpconnect.njall.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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

  @Test
  @DisplayName("provideServerConfig parses custom configuration trees")
  void provideServerConfig_withCustomConfig_parsesAllFields() {
    var customConfig =
        ConfigFactory.parseString(
            """
            larpconnect.server.host = "10.0.0.1"
            larpconnect.server.port = 9000
            larpconnect.server.name = "custom-node"
            larpconnect.server.primary-domain = "example.org"
            larpconnect.server.admin-contact = "contact@example.org"
            """);
    var injector = Guice.createInjector(new ConfigModule(customConfig));
    var serverConfig = injector.getInstance(ServerConfig.class);

    assertThat(serverConfig.host()).isEqualTo("10.0.0.1");
    assertThat(serverConfig.port()).isEqualTo(9000);
    assertThat(serverConfig.name()).isEqualTo("custom-node");
    assertThat(serverConfig.primaryDomain()).isEqualTo("example.org");
    assertThat(serverConfig.adminContact()).isEqualTo("contact@example.org");
  }

  @Test
  @DisplayName("provideServerConfig uses defaults when optional metadata is missing")
  void provideServerConfig_missingMetadata_usesDefaults() {
    var minimalConfig =
        ConfigFactory.parseString(
            """
            larpconnect.server.host = "127.0.0.1"
            larpconnect.server.port = 8080
            """);
    var injector = Guice.createInjector(new ConfigModule(minimalConfig));
    var serverConfig = injector.getInstance(ServerConfig.class);

    assertThat(serverConfig.host()).isEqualTo("127.0.0.1");
    assertThat(serverConfig.port()).isEqualTo(8080);
    assertThat(serverConfig.name()).isEqualTo(ServerConfig.DEFAULT_NAME);
    assertThat(serverConfig.primaryDomain()).isEqualTo(ServerConfig.DEFAULT_PRIMARY_DOMAIN);
    assertThat(serverConfig.adminContact()).isEqualTo(ServerConfig.DEFAULT_ADMIN_CONTACT);
  }
}
