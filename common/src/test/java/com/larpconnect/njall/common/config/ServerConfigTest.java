package com.larpconnect.njall.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerConfigTest {

  @Test
  @DisplayName("Constructor creates record with valid host and port using defaults")
  void constructor_twoParameters_createsRecordWithDefaults() {
    var config = new ServerConfig("127.0.0.1", 8080);

    assertThat(config.host()).isEqualTo("127.0.0.1");
    assertThat(config.port()).isEqualTo(8080);
    assertThat(config.name()).isEqualTo("default-server");
    assertThat(config.primaryDomain()).isEqualTo("larpconnect.org");
    assertThat(config.adminContact()).isEqualTo("admin@larpconnect.org");
  }

  @Test
  @DisplayName("Constructor creates record with explicit metadata parameters")
  void constructor_fullParameters_createsRecord() {
    var config = new ServerConfig("10.0.0.1", 9000, "node-1", "larp.test", "admin@larp.test");

    assertThat(config.host()).isEqualTo("10.0.0.1");
    assertThat(config.port()).isEqualTo(9000);
    assertThat(config.name()).isEqualTo("node-1");
    assertThat(config.primaryDomain()).isEqualTo("larp.test");
    assertThat(config.adminContact()).isEqualTo("admin@larp.test");
  }

  @Test
  @DisplayName("Constructor throws IllegalArgumentException when port is negative")
  void constructor_negativePort_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> new ServerConfig("localhost", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Port must be between 0 and 65535");
  }

  @Test
  @DisplayName("Constructor throws IllegalArgumentException when port exceeds 65535")
  void constructor_portExceedsMax_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> new ServerConfig("localhost", 65536))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Port must be between 0 and 65535");
  }
}
