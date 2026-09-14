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

  @Test
  @DisplayName("of creates DatabaseConfig with valid MigrationConfig")
  void of_validMigrationConfig_createsRecord() {
    var dbConfig = DatabaseConfig.of(VALID_MIGRATION);

    assertThat(dbConfig.migration()).isSameAs(VALID_MIGRATION);
  }

  @Test
  @DisplayName("of throws NullPointerException when migration is null")
  void of_nullMigrationConfig_throwsNullPointerException() {
    assertThatThrownBy(() -> DatabaseConfig.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("migration cannot be null");
  }

  @Test
  @DisplayName("fromConfig parses DatabaseConfig correctly")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            "larpconnect.data.database.migration {\n"
                + "  jdbc-url = \"jdbc:postgresql://localhost:5432/app\"\n"
                + "  username = \"njall\"\n"
                + "  password = \"secret\"\n"
                + "  schemas = [\"njall\"]\n"
                + "  default-schema = \"njall\"\n"
                + "}");
    var serverConfig = ServerConfig.of("127.0.0.1", 8080);

    var dbConfig = DatabaseConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(dbConfig.migration().jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/app");
    assertThat(dbConfig.migration().username()).isEqualTo("njall");
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
