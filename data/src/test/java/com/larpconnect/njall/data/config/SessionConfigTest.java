package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class SessionConfigTest {

  private static final String TEST_URL = "jdbc:postgresql://localhost:5432/db";

  @Test
  @DisplayName("of creates SessionConfig with valid parameters")
  void of_validParameters_createsRecord() {
    var config = SessionConfig.of(TEST_URL, "admin", "secret", 2, 10, 5);

    assertThat(config.jdbcUrl()).isEqualTo(TEST_URL);
    assertThat(config.username()).isEqualTo("admin");
    assertThat(config.password()).isEqualTo("secret");
    assertThat(config.hasPassword()).isTrue();
    assertThat(config.minPoolSize()).isEqualTo(2);
    assertThat(config.maxPoolSize()).isEqualTo(10);
    assertThat(config.timeoutSeconds()).isEqualTo(5);
  }

  @Test
  @DisplayName("of allows null, empty, or blank password with hasPassword returning false")
  void of_nullOrBlankPassword_setsPasswordAndHasPasswordFalse() {
    var nullConfig = SessionConfig.of(TEST_URL, "admin", null, 2, 10, 5);
    assertThat(nullConfig.password()).isNull();
    assertThat(nullConfig.hasPassword()).isFalse();

    var emptyConfig = SessionConfig.of(TEST_URL, "admin", "", 2, 10, 5);
    assertThat(emptyConfig.password()).isEmpty();
    assertThat(emptyConfig.hasPassword()).isFalse();

    var blankConfig = SessionConfig.of(TEST_URL, "admin", "   ", 2, 10, 5);
    assertThat(blankConfig.password()).isEqualTo("   ");
    assertThat(blankConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName("of throws NullPointerException when required string is null")
  void of_nullString_throwsNullPointerException() {
    assertThatThrownBy(() -> SessionConfig.of(null, "admin", "secret", 2, 10, 5))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("jdbcUrl cannot be null");
    assertThatThrownBy(() -> SessionConfig.of(TEST_URL, null, "secret", 2, 10, 5))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("username cannot be null");
  }

  @Test
  @DisplayName("of throws IllegalArgumentException when pool sizes are invalid")
  void of_invalidPoolSizes_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> SessionConfig.of(TEST_URL, "admin", "secret", 0, 10, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minPoolSize must be at least 1");

    assertThatThrownBy(() -> SessionConfig.of(TEST_URL, "admin", "secret", 10, 2, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxPoolSize must be greater than or equal to minPoolSize");

    assertThatThrownBy(() -> SessionConfig.of(TEST_URL, "admin", "secret", 2, 10, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timeoutSeconds must be at least 1");
  }

  @Test
  @DisplayName("fromConfig parses SessionConfig from Typesafe Config")
  void fromConfig_validConfig_parsesCorrectly() {
    var rawConfig =
        ConfigFactory.parseString(
            "admin {\n"
                + "  jdbc-url = \"jdbc:postgresql://localhost:5432/test\"\n"
                + "  username = \"njall_admin\"\n"
                + "  password = \"admin_pass\"\n"
                + "  pool {\n"
                + "    min-size = 3\n"
                + "    max-size = 15\n"
                + "    timeout-seconds = 10\n"
                + "  }\n"
                + "}");

    var sessionConfig = SessionConfig.fromConfig(rawConfig, "admin");

    assertThat(sessionConfig.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/test");
    assertThat(sessionConfig.username()).isEqualTo("njall_admin");
    assertThat(sessionConfig.password()).isEqualTo("admin_pass");
    assertThat(sessionConfig.hasPassword()).isTrue();
    assertThat(sessionConfig.minPoolSize()).isEqualTo(3);
    assertThat(sessionConfig.maxPoolSize()).isEqualTo(15);
    assertThat(sessionConfig.timeoutSeconds()).isEqualTo(10);
  }

  @Test
  @DisplayName("fromConfig parses SessionConfig when password is omitted")
  void fromConfig_omittedPassword_parsesWithNullPassword() {
    var rawConfig =
        ConfigFactory.parseString(
            "admin {\n"
                + "  jdbc-url = \"jdbc:postgresql://localhost:5432/test\"\n"
                + "  username = \"njall_admin\"\n"
                + "  pool {\n"
                + "    min-size = 3\n"
                + "    max-size = 15\n"
                + "    timeout-seconds = 10\n"
                + "  }\n"
                + "}");

    var sessionConfig = SessionConfig.fromConfig(rawConfig, "admin");

    assertThat(sessionConfig.password()).isNull();
    assertThat(sessionConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName("fromConfig parses SessionConfig when password is empty string")
  void fromConfig_emptyPassword_parsesWithEmptyPassword() {
    var rawConfig =
        ConfigFactory.parseString(
            "admin {\n"
                + "  jdbc-url = \"jdbc:postgresql://localhost:5432/test\"\n"
                + "  username = \"njall_admin\"\n"
                + "  password = \"\"\n"
                + "  pool {\n"
                + "    min-size = 3\n"
                + "    max-size = 15\n"
                + "    timeout-seconds = 10\n"
                + "  }\n"
                + "}");

    var sessionConfig = SessionConfig.fromConfig(rawConfig, "admin");

    assertThat(sessionConfig.password()).isEmpty();
    assertThat(sessionConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when arguments are null")
  void fromConfig_nullArguments_throwsNullPointerException() {
    var config = ConfigFactory.empty();
    assertThatThrownBy(() -> SessionConfig.fromConfig(null, "admin"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("config cannot be null");
    assertThatThrownBy(() -> SessionConfig.fromConfig(config, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("path cannot be null");
  }
}
