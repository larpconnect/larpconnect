package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultSessionConfigFactoryTest {

  @Test
  @DisplayName("create parses SessionConfig from Typesafe Config")
  void create_validConfig_parsesCorrectly() {
    var rawConfig =
        ConfigFactory.parseString(
            """
            admin {
              jdbc-url = "jdbc:postgresql://localhost:5432/test"
              username = "njall_admin"
              password = "admin_pass"
              pool {
                min-size = 3
                max-size = 15
                timeout-seconds = 10
              }
            }
            """);

    var factory = new DefaultSessionConfigFactory(rawConfig);
    var sessionConfig = factory.create("admin");

    assertThat(sessionConfig.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/test");
    assertThat(sessionConfig.username()).isEqualTo("njall_admin");
    assertThat(sessionConfig.password()).contains("admin_pass");
    assertThat(sessionConfig.trustAuth()).isFalse();
    assertThat(sessionConfig.hasPassword()).isTrue();
    assertThat(sessionConfig.minPoolSize()).isEqualTo(3);
    assertThat(sessionConfig.maxPoolSize()).isEqualTo(15);
    assertThat(sessionConfig.timeoutSeconds()).isEqualTo(10);
  }

  @Test
  @DisplayName("create parses SessionConfig when password is omitted but trustAuth is true")
  void create_omittedPasswordWithTrustAuth_parsesSuccessfully() {
    var rawConfig =
        ConfigFactory.parseString(
            """
            admin {
              jdbc-url = "jdbc:postgresql://localhost:5432/test"
              username = "njall_admin"
              trust-auth = true
              pool {
                min-size = 3
                max-size = 15
                timeout-seconds = 10
              }
            }
            """);

    var factory = new DefaultSessionConfigFactory(rawConfig);
    var sessionConfig = factory.create("admin");

    assertThat(sessionConfig.password()).isEmpty();
    assertThat(sessionConfig.trustAuth()).isTrue();
    assertThat(sessionConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName("create parses SessionConfig when password is empty string but trustAuth is true")
  void create_emptyPasswordWithTrustAuth_parsesSuccessfully() {
    var rawConfig =
        ConfigFactory.parseString(
            """
            admin {
              jdbc-url = "jdbc:postgresql://localhost:5432/test"
              username = "njall_admin"
              password = ""
              trust-auth = true
              pool {
                min-size = 3
                max-size = 15
                timeout-seconds = 10
              }
            }
            """);

    var factory = new DefaultSessionConfigFactory(rawConfig);
    var sessionConfig = factory.create("admin");

    assertThat(sessionConfig.password()).contains("");
    assertThat(sessionConfig.trustAuth()).isTrue();
    assertThat(sessionConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName("create throws IllegalStateException when password is blank and trustAuth is false")
  void create_blankPasswordWithoutTrustAuth_throwsIllegalStateException() {
    var rawConfig =
        ConfigFactory.parseString(
            """
            admin {
              jdbc-url = "jdbc:postgresql://localhost:5432/test"
              username = "njall_admin"
              password = ""
              pool {
                min-size = 3
                max-size = 15
                timeout-seconds = 10
              }
            }
            """);

    var factory = new DefaultSessionConfigFactory(rawConfig);
    assertThatThrownBy(() -> factory.create("admin"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for profile with username 'njall_admin'");
  }

  @Test
  @DisplayName("create inherits global trust-auth when profile does not declare one")
  void create_globalTrustAuth_inheritsSetting() {
    var rawConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database.trust-auth = true
            admin {
              jdbc-url = "jdbc:postgresql://localhost:5432/test"
              username = "njall_admin"
              password = ""
              pool {
                min-size = 3
                max-size = 15
                timeout-seconds = 10
              }
            }
            """);

    var factory = new DefaultSessionConfigFactory(rawConfig);
    var sessionConfig = factory.create("admin");
    assertThat(sessionConfig.trustAuth()).isTrue();
  }

  @Test
  @DisplayName("create profile trust-auth overrides global setting")
  void create_profileOverride_takesPrecedenceOverGlobal() {
    var rawConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database.trust-auth = true
            admin {
              jdbc-url = "jdbc:postgresql://localhost:5432/test"
              username = "njall_admin"
              password = ""
              trust-auth = false
              pool {
                min-size = 3
                max-size = 15
                timeout-seconds = 10
              }
            }
            """);

    var factory = new DefaultSessionConfigFactory(rawConfig);
    assertThatThrownBy(() -> factory.create("admin"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for profile with username 'njall_admin'");
  }
}
