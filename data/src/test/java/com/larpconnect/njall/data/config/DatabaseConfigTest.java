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
  @DisplayName("of throws NullPointerException when arguments are null")
  void of_nullArguments_throwsNullPointerException() {
    assertThatThrownBy(() -> DatabaseConfig.of(null, VALID_ADMIN, VALID_USERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("migration cannot be null");
    assertThatThrownBy(() -> DatabaseConfig.of(VALID_MIGRATION, null, VALID_USERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("admin cannot be null");
    assertThatThrownBy(() -> DatabaseConfig.of(VALID_MIGRATION, VALID_ADMIN, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("users cannot be null");
  }

  @Test
  @DisplayName("fromConfig parses DatabaseConfig correctly")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            "larpconnect.data.database {\n"
                + "  migration {\n"
                + "    jdbc-url = \"jdbc:postgresql://localhost:5432/app\"\n"
                + "    username = \"njall\"\n"
                + "    password = \"secret\"\n"
                + "    schemas = [\"njall\"]\n"
                + "    default-schema = \"njall\"\n"
                + "  }\n"
                + "  admin {\n"
                + "    jdbc-url = \"jdbc:postgresql://localhost:5432/app\"\n"
                + "    username = \"njall_admin\"\n"
                + "    password = \"secret_admin\"\n"
                + "    pool { min-size = 2, max-size = 10, timeout-seconds = 5 }\n"
                + "  }\n"
                + "  users {\n"
                + "    jdbc-url = \"jdbc:postgresql://localhost:5432/app\"\n"
                + "    username = \"njall_users\"\n"
                + "    password = \"secret_users\"\n"
                + "    pool { min-size = 5, max-size = 20, timeout-seconds = 5 }\n"
                + "  }\n"
                + "}");
    var serverConfig = ServerConfig.of("127.0.0.1", 8080);

    var dbConfig = DatabaseConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(dbConfig.migration().jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/app");
    assertThat(dbConfig.migration().username()).isEqualTo("njall");
    assertThat(dbConfig.admin().username()).isEqualTo("njall_admin");
    assertThat(dbConfig.users().username()).isEqualTo("njall_users");
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when config is null")
  void fromConfig_nullConfig_throwsNullPointerException() {
    var serverConfig = ServerConfig.of("127.0.0.1", 8080);
    assertThatThrownBy(() -> DatabaseConfig.fromConfig(null, serverConfig))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("config cannot be null");
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when serverConfig is null")
  void fromConfig_nullServerConfig_throwsNullPointerException() {
    var config = ConfigFactory.empty();
    assertThatThrownBy(() -> DatabaseConfig.fromConfig(config, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("serverConfig cannot be null");
  }
}
