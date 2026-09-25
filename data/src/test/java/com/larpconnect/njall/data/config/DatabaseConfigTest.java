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
      new MigrationConfig(
          "jdbc:postgresql://localhost:5432/db",
          "user",
          "pass",
          List.of("s1"),
          "s1",
          Map.of("k", "v"));

  private static final SessionConfig VALID_ADMIN =
      new SessionConfig("jdbc:postgresql://localhost:5432/db", "admin", "pass", 2, 10, 5);

  private static final SessionConfig VALID_USERS =
      new SessionConfig("jdbc:postgresql://localhost:5432/db", "users", "pass", 5, 20, 5);

  private static DatabaseConfig createDatabaseConfig(
      com.typesafe.config.Config typesafeConfig, ServerConfig serverConfig) {
    var module = new DatabaseConfigModule();
    var factory = new DefaultSessionConfigFactory(typesafeConfig);
    var migrationConfig = module.provideMigrationConfig(typesafeConfig, serverConfig);
    var adminConfig = module.provideAdminSessionConfig(factory);
    var usersConfig = module.provideUsersSessionConfig(factory);
    return module.provideDatabaseConfig(migrationConfig, adminConfig, usersConfig);
  }

  @Test
  @DisplayName("constructor creates DatabaseConfig with valid configs")
  void constructor_validConfigs_createsRecord() {
    var dbConfig = new DatabaseConfig(VALID_MIGRATION, VALID_ADMIN, VALID_USERS);

    assertThat(dbConfig.migration()).isSameAs(VALID_MIGRATION);
    assertThat(dbConfig.admin()).isSameAs(VALID_ADMIN);
    assertThat(dbConfig.users()).isSameAs(VALID_USERS);
  }

  @Test
  @DisplayName("DatabaseConfigModule provides DatabaseConfig correctly from Typesafe Config")
  void createDatabaseConfig_validConfig_parsesCorrectly() {
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
            }
            """);
    var serverConfig = new ServerConfig("127.0.0.1", 8080);

    var dbConfig = createDatabaseConfig(typesafeConfig, serverConfig);

    assertThat(dbConfig.migration().jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/app");
    assertThat(dbConfig.migration().username()).isEqualTo("njall");
    assertThat(dbConfig.admin().username()).isEqualTo("njall_admin");
    assertThat(dbConfig.users().username()).isEqualTo("njall_users");
  }

  @Test
  @DisplayName("DatabaseConfigModule creates DatabaseConfig with global trust-auth enabled")
  void createDatabaseConfig_globalTrustAuth_parsesEmptyPasswordsSuccessfully() {
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
            }
            """);
    var serverConfig = new ServerConfig("127.0.0.1", 8080);

    var dbConfig = createDatabaseConfig(typesafeConfig, serverConfig);

    assertThat(dbConfig.migration().trustAuth()).isTrue();
    assertThat(dbConfig.admin().trustAuth()).isTrue();
    assertThat(dbConfig.users().trustAuth()).isTrue();
  }

  @Test
  @DisplayName("DatabaseConfigModule throws when trust-auth is false and password is empty")
  void createDatabaseConfig_unconfiguredPasswordWithoutTrustAuth_throwsIllegalStateException() {
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
            }
            """);
    var serverConfig = new ServerConfig("127.0.0.1", 8080);

    assertThatThrownBy(() -> createDatabaseConfig(typesafeConfig, serverConfig))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for migration profile with username 'njall'");
  }
}
