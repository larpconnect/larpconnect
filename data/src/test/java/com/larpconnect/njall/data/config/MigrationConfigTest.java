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
    assertThat(config.schemas()).containsExactlyElementsOf(VALID_SCHEMAS);
    assertThat(config.defaultSchema()).isEqualTo(VALID_DEFAULT_SCHEMA);
    assertThat(config.placeholders()).containsEntry("server_name", "test-srv");
  }

  @Test
  @DisplayName("of throws NullPointerException when jdbcUrl is null")
  void of_nullJdbcUrl_throwsNullPointerException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    null,
                    VALID_USER,
                    VALID_PASSWORD,
                    VALID_SCHEMAS,
                    VALID_DEFAULT_SCHEMA,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("jdbcUrl cannot be null");
  }

  @Test
  @DisplayName("of throws NullPointerException when username is null")
  void of_nullUsername_throwsNullPointerException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    null,
                    VALID_PASSWORD,
                    VALID_SCHEMAS,
                    VALID_DEFAULT_SCHEMA,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("username cannot be null");
  }

  @Test
  @DisplayName("of throws NullPointerException when password is null")
  void of_nullPassword_throwsNullPointerException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    VALID_USER,
                    null,
                    VALID_SCHEMAS,
                    VALID_DEFAULT_SCHEMA,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("password cannot be null");
  }

  @Test
  @DisplayName("of throws NullPointerException when schemas is null")
  void of_nullSchemas_throwsNullPointerException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    VALID_USER,
                    VALID_PASSWORD,
                    null,
                    VALID_DEFAULT_SCHEMA,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("schemas cannot be null");
  }

  @Test
  @DisplayName("of throws NullPointerException when defaultSchema is null")
  void of_nullDefaultSchema_throwsNullPointerException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    VALID_USER,
                    VALID_PASSWORD,
                    VALID_SCHEMAS,
                    null,
                    VALID_PLACEHOLDERS))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("defaultSchema cannot be null");
  }

  @Test
  @DisplayName("of throws NullPointerException when placeholders is null")
  void of_nullPlaceholders_throwsNullPointerException() {
    assertThatThrownBy(
            () ->
                MigrationConfig.of(
                    VALID_JDBC_URL,
                    VALID_USER,
                    VALID_PASSWORD,
                    VALID_SCHEMAS,
                    VALID_DEFAULT_SCHEMA,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("placeholders cannot be null");
  }

  @Test
  @DisplayName("fromConfig parses MigrationConfig from valid Config and ServerConfig")
  void fromConfig_validConfig_parsesCorrectly() {
    var typesafeConfig =
        ConfigFactory.parseString(
            "larpconnect.data.database.migration {\n"
                + "  jdbc-url = \"jdbc:postgresql://db:5432/app\"\n"
                + "  username = \"app_migrator\"\n"
                + "  password = \"pass123\"\n"
                + "  schemas = [\"s1\", \"s2\"]\n"
                + "  default-schema = \"s1\"\n"
                + "}");
    var serverConfig =
        ServerConfig.of("0.0.0.0", 8080, "my-server", "my-domain.com", "ops@my-domain.com");

    var config = MigrationConfig.fromConfig(typesafeConfig, serverConfig);

    assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/app");
    assertThat(config.username()).isEqualTo("app_migrator");
    assertThat(config.password()).isEqualTo("pass123");
    assertThat(config.schemas()).containsExactly("s1", "s2");
    assertThat(config.defaultSchema()).isEqualTo("s1");
    assertThat(config.placeholders())
        .containsEntry("server_name", "my-server")
        .containsEntry("primary_domain", "my-domain.com")
        .containsEntry("admin_contact", "ops@my-domain.com");
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when config is null")
  void fromConfig_nullConfig_throwsNullPointerException() {
    var serverConfig = ServerConfig.of("0.0.0.0", 8080);
    assertThatThrownBy(() -> MigrationConfig.fromConfig(null, serverConfig))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("config cannot be null");
  }

  @Test
  @DisplayName("fromConfig throws NullPointerException when serverConfig is null")
  void fromConfig_nullServerConfig_throwsNullPointerException() {
    var typesafeConfig = ConfigFactory.empty();
    assertThatThrownBy(() -> MigrationConfig.fromConfig(typesafeConfig, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("serverConfig cannot be null");
  }
}
