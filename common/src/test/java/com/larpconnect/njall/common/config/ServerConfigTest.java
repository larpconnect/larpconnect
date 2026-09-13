package com.larpconnect.njall.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerConfigTest {

  @Test
  @DisplayName("of creates record with valid host and port")
  void of_validParameters_createsRecord() {
    var config = ServerConfig.of("127.0.0.1", 8080);

    assertThat(config.host()).isEqualTo("127.0.0.1");
    assertThat(config.port()).isEqualTo(8080);
  }

  @Test
  @DisplayName("of throws NullPointerException when host is null")
  void of_nullHost_throwsNullPointerException() {
    assertThatThrownBy(() -> ServerConfig.of(null, 8080))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("host cannot be null");
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
  @DisplayName("fromConfig parses host and port from valid Config")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            "larpconnect.server.host = \"10.0.0.1\"\nlarpconnect.server.port = 9000");
    var config = ServerConfig.fromConfig(typesafeConfig);

    assertThat(config.host()).isEqualTo("10.0.0.1");
    assertThat(config.port()).isEqualTo(9000);
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when config is null")
  void fromConfig_nullConfig_throwsNullPointerException() {
    assertThatThrownBy(() -> ServerConfig.fromConfig(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("config cannot be null");
  }
}
