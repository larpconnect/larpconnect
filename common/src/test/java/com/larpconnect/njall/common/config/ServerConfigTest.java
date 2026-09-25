package com.larpconnect.njall.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerConfigTest {

  @Test
  @DisplayName("of creates record with valid host and port using defaults")
  void of_validParameters_createsRecordWithDefaults() {
    var config = ServerConfig.of("127.0.0.1", 8080);

    assertThat(config.host()).isEqualTo("127.0.0.1");
    assertThat(config.port()).isEqualTo(8080);
    assertThat(config.name()).isEqualTo("default-server");
    assertThat(config.primaryDomain()).isEqualTo("larpconnect.org");
    assertThat(config.adminContact()).isEqualTo("admin@larpconnect.org");
  }

  @Test
  @DisplayName("of creates record with explicit metadata parameters")
  void of_fullParameters_createsRecord() {
    var config = ServerConfig.of("10.0.0.1", 9000, "node-1", "larp.test", "admin@larp.test");

    assertThat(config.host()).isEqualTo("10.0.0.1");
    assertThat(config.port()).isEqualTo(9000);
    assertThat(config.name()).isEqualTo("node-1");
    assertThat(config.primaryDomain()).isEqualTo("larp.test");
    assertThat(config.adminContact()).isEqualTo("admin@larp.test");
  }

  @Test
  @DisplayName("of throws IllegalArgumentException when port is negative")
  void of_negativePort_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> ServerConfig.of("localhost", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Port must be between 0 and 65535");
  }

  @Test
  @DisplayName("of throws IllegalArgumentException when port exceeds 65535")
  void of_portExceedsMax_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> ServerConfig.of("localhost", 65536))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Port must be between 0 and 65535");
  }

  @Test
  @DisplayName("fromConfig parses host, port, and metadata from valid Config")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.server.host = "10.0.0.1"
            larpconnect.server.port = 9000
            larpconnect.server.name = "custom-node"
            larpconnect.server.primary-domain = "example.org"
            larpconnect.server.admin-contact = "contact@example.org"
            """);
    var config = ServerConfig.fromConfig(typesafeConfig);

    assertThat(config.host()).isEqualTo("10.0.0.1");
    assertThat(config.port()).isEqualTo(9000);
    assertThat(config.name()).isEqualTo("custom-node");
    assertThat(config.primaryDomain()).isEqualTo("example.org");
    assertThat(config.adminContact()).isEqualTo("contact@example.org");
  }

  @Test
  @DisplayName("fromConfig uses defaults when optional metadata is missing")
  void fromConfig_missingMetadata_usesDefaults() {
    var typesafeConfig =
        ConfigFactory.parseString(
            "larpconnect.server.host = \"127.0.0.1\"\nlarpconnect.server.port = 8080");
    var config = ServerConfig.fromConfig(typesafeConfig);

    assertThat(config.host()).isEqualTo("127.0.0.1");
    assertThat(config.port()).isEqualTo(8080);
    assertThat(config.name()).isEqualTo("default-server");
    assertThat(config.primaryDomain()).isEqualTo("larpconnect.org");
    assertThat(config.adminContact()).isEqualTo("admin@larpconnect.org");
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when config is null")
  void fromConfig_nullConfig_throwsNullPointerException() {
    assertThatThrownBy(() -> ServerConfig.fromConfig(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("config cannot be null");
  }
}
