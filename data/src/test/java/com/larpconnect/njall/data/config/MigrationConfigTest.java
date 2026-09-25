package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larpconnect.njall.common.config.ServerConfig;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class MigrationConfigTest {

  private static final String VALID_JDBC_URL = "jdbc:postgresql://localhost:5432/larpconnect";
  private static final String VALID_USER = "njall";
  private static final String VALID_PASSWORD = "secret";
  private static final List<String> VALID_SCHEMAS =
      List.of("njall", "njall_admin", "njall_users", "njall_system");
  private static final String VALID_DEFAULT_SCHEMA = "njall";
  private static final Map<String, String> VALID_PLACEHOLDERS =
      Map.of(
          "server_name", "test-srv", "primary_domain", "test.org", "admin_contact", "admin@test");

  @Test
  @DisplayName("of creates MigrationConfig with valid parameters")
  void of_validParameters_createsRecord() {
    var config =
        MigrationConfig.of(
            VALID_JDBC_URL,
            VALID_USER,
            VALID_PASSWORD,
            VALID_SCHEMAS,
            VALID_DEFAULT_SCHEMA,
            VALID_PLACEHOLDERS);

    assertThat(config.jdbcUrl()).isEqualTo(VALID_JDBC_URL);
    assertThat(config.username()).isEqualTo(VALID_USER);
    assertThat(config.password()).isEqualTo(VALID_PASSWORD);
    assertThat(config.trustAuth()).isFalse();
    assertThat(config.hasPassword()).isTrue();
    assertThat(config.schemas()).containsExactlyElementsOf(VALID_SCHEMAS);
    assertThat(config.defaultSchema()).isEqualTo(VALID_DEFAULT_SCHEMA);
    assertThat(config.placeholders()).containsEntry("server_name", "test-srv");
  }

  @Test
  @DisplayName("of with explicit trustAuth allows null, empty, or blank password")
  void of_nullOrBlankPasswordWithTrustAuth_setsPasswordAndHasPasswordFalse() {
    var nullConfig =
        MigrationConfig.of(
            VALID_JDBC_URL,
            VALID_USER,
            null,
            true,
            VALID_SCHEMAS,
            VALID_DEFAULT_SCHEMA,
            VALID_PLACEHOLDERS);
    assertThat(nullConfig.password()).isNull();
    assertThat(nullConfig.trustAuth()).isTrue();
    assertThat(nullConfig.hasPassword()).isFalse();

    var emptyConfig =
        MigrationConfig.of(
            VALID_JDBC_URL,
            VALID_USER,
            "",
            true,
            VALID_SCHEMAS,
            VALID_DEFAULT_SCHEMA,
            VALID_PLACEHOLDERS);
    assertThat(emptyConfig.password()).isEmpty();
    assertThat(emptyConfig.trustAuth()).isTrue();
    assertThat(emptyConfig.hasPassword()).isFalse();

    var blankConfig =
        MigrationConfig.of(
            VALID_JDBC_URL,
            VALID_USER,
            "   ",
            true,
            VALID_SCHEMAS,
            VALID_DEFAULT_SCHEMA,
            VALID_PLACEHOLDERS);
    assertThat(blankConfig.password()).isEqualTo("   ");
    assertThat(blankConfig.trustAuth()).isTrue();
    assertThat(blankConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName(
      "of throws IllegalStateException when password is null or blank and trustAuth is false")
  void of_nullOrBlankPasswordWithoutTrustAuth_throwsIllegalStateException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    VALID_USER,
                    null,
                    false,
                    VALID_SCHEMAS,
                    VALID_DEFAULT_SCHEMA,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for migration profile with username 'njall'");

    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    VALID_USER,
                    "",
                    false,
                    VALID_SCHEMAS,
                    VALID_DEFAULT_SCHEMA,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for migration profile with username 'njall'");
  }

  @Test
  @DisplayName("fromConfig parses MigrationConfig from valid Config and ServerConfig")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database.migration {
              jdbc-url = "jdbc:postgresql://db:5432/app"
              username = "app_migrator"
              password = "pass123"
              schemas = ["s1", "s2"]
              default-schema = "s1"
            }
            """);
    var serverConfig =
        ServerConfig.of("0.0.0.0", 8080, "my-server", "my-domain.com", "ops@my-domain.com");

    var config = MigrationConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/app");
    assertThat(config.username()).isEqualTo("app_migrator");
    assertThat(config.password()).isEqualTo("pass123");
    assertThat(config.trustAuth()).isFalse();
    assertThat(config.hasPassword()).isTrue();
    assertThat(config.schemas()).containsExactly("s1", "s2");
    assertThat(config.defaultSchema()).isEqualTo("s1");
    assertThat(config.placeholders())
        .containsEntry("server_name", "my-server")
        .containsEntry("primary_domain", "my-domain.com")
        .containsEntry("admin_contact", "ops@my-domain.com");
  }

  @Test
  @DisplayName("fromConfig parses MigrationConfig when password is empty but trustAuth is true")
  void fromConfig_emptyPasswordWithTrustAuth_parsesSuccessfully() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database.migration {
              jdbc-url = "jdbc:postgresql://db:5432/app"
              username = "app_migrator"
              password = ""
              trust-auth = true
              schemas = ["s1", "s2"]
              default-schema = "s1"
            }
            """);
    var serverConfig =
        ServerConfig.of("0.0.0.0", 8080, "my-server", "my-domain.com", "ops@my-domain.com");

    var config = MigrationConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(config.password()).isEmpty();
    assertThat(config.trustAuth()).isTrue();
    assertThat(config.hasPassword()).isFalse();
  }

  @Test
  @DisplayName(
      "fromConfig throws IllegalStateException when password is empty and trustAuth is false")
  void fromConfig_emptyPasswordWithoutTrustAuth_throwsIllegalStateException() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database.migration {
              jdbc-url = "jdbc:postgresql://db:5432/app"
              username = "app_migrator"
              password = ""
              schemas = ["s1", "s2"]
              default-schema = "s1"
            }
            """);
    var serverConfig =
        ServerConfig.of("0.0.0.0", 8080, "my-server", "my-domain.com", "ops@my-domain.com");

    assertThatThrownBy(() -> MigrationConfig.fromConfig(typesafeConfig, serverConfig))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Database password is required for migration profile with username 'app_migrator'");
  }

  @Test
  @DisplayName("fromConfig inherits global trust-auth when migration section does not declare one")
  void fromConfig_globalTrustAuth_inheritsSetting() {
    var typesafeConfig =
        ConfigFactory.parseString(
            """
            larpconnect.data.database.trust-auth = true
            larpconnect.data.database.migration {
              jdbc-url = "jdbc:postgresql://db:5432/app"
              username = "app_migrator"
              password = ""
              schemas = ["s1", "s2"]
              default-schema = "s1"
            }
            """);
    var serverConfig =
        ServerConfig.of("0.0.0.0", 8080, "my-server", "my-domain.com", "ops@my-domain.com");

    var config = MigrationConfig.fromConfig(typesafeConfig, serverConfig);
    assertThat(config.trustAuth()).isTrue();
  }
}
