package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larpconnect.njall.common.config.ServerConfig;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DatabaseConfigTest {

  private static final MigrationConfig VALID_MIGRATION =
      MigrationConfig.of(
          "jdbc:postgresql://localhost:5432/db",
          "user",
          "pass",
          List.of("s1"),
          "s1",
          Map.of("k", "v"));

  private static final SessionConfig VALID_ADMIN =
      SessionConfig.of("jdbc:postgresql://localhost:5432/db", "admin", "pass", 2, 10, 5);

  private static final SessionConfig VALID_USERS =
      SessionConfig.of("jdbc:postgresql://localhost:5432/db", "users", "pass", 5, 20, 5);

  @Test
  @DisplayName("of creates DatabaseConfig with valid configs")
  void of_validConfigs_createsRecord() {
    var dbConfig = DatabaseConfig.of(VALID_MIGRATION, VALID_ADMIN, VALID_USERS);

    assertThat(dbConfig.migration()).isSameAs(VALID_MIGRATION);
    assertThat(dbConfig.admin()).isSameAs(VALID_ADMIN);
    assertThat(dbConfig.users()).isSameAs(VALID_USERS);
  }

  @Test
  @DisplayName("fromConfig parses DatabaseConfig correctly")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database {
              migration {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall"
                password = "secret"
                schemas = ["njall"]
                default-schema = "njall"
              }
              admin {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall_admin"
                password = "secret_admin"
                pool { min-size = 2, max-size = 10, timeout-seconds = 5 }
              }
              users {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall_users"
                password = "secret_users"
                pool { min-size = 5, max-size = 20, timeout-seconds = 5 }
              }
            }\
            """);
    var serverConfig = ServerConfig.of("127.0.0.1", 8080);

    var dbConfig = DatabaseConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(dbConfig.migration().jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/app");
    assertThat(dbConfig.migration().username()).isEqualTo("njall");
    assertThat(dbConfig.admin().username()).isEqualTo("njall_admin");
    assertThat(dbConfig.users().username()).isEqualTo("njall_users");
  }

  @Test
  @DisplayName("fromConfig parses DatabaseConfig with global trust-auth enabled")
  void fromConfig_globalTrustAuth_parsesEmptyPasswordsSuccessfully() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database {
              trust-auth = true
              migration {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall"
                password = ""
                schemas = ["njall"]
                default-schema = "njall"
              }
              admin {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall_admin"
                password = ""
                pool { min-size = 2, max-size = 10, timeout-seconds = 5 }
              }
              users {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall_users"
                password = ""
                pool { min-size = 5, max-size = 20, timeout-seconds = 5 }
              }
            }\
            """);
    var serverConfig = ServerConfig.of("127.0.0.1", 8080);

    var dbConfig = DatabaseConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(dbConfig.migration().trustAuth()).isTrue();
    assertThat(dbConfig.admin().trustAuth()).isTrue();
    assertThat(dbConfig.users().trustAuth()).isTrue();
  }

  @Test
  @DisplayName(
      "fromConfig throws IllegalStateException when trust-auth is false and password is empty")
  void fromConfig_unconfiguredPasswordWithoutTrustAuth_throwsIllegalStateException() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database {
              trust-auth = false
              migration {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall"
                password = ""
                schemas = ["njall"]
                default-schema = "njall"
              }
              admin {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall_admin"
                password = "secret_admin"
                pool { min-size = 2, max-size = 10, timeout-seconds = 5 }
              }
              users {
                jdbc-url = "jdbc:postgresql://localhost:5432/app"
                username = "njall_users"
                password = "secret_users"
                pool { min-size = 5, max-size = 20, timeout-seconds = 5 }
              }
            }\
            """);
    var serverConfig = ServerConfig.of("127.0.0.1", 8080);

    assertThatThrownBy(() -> DatabaseConfig.fromConfig(typesafeConfig, serverConfig))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for migration profile with username 'njall'");
  }
}
