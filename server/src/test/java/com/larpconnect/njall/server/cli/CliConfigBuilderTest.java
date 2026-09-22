package com.larpconnect.njall.server.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CliConfigBuilderTest {

  @Test
  @DisplayName("build with defaults returns unmodified base config")
  void build_withDefaults_returnsBaseConfig() {
    var base = ConfigFactory.parseString("larpconnect.server.port = 8080");
    var builder = new CliConfigBuilder(base);

    var config = builder.build();

    assertThat(config.getInt("larpconnect.server.port")).isEqualTo(8080);
  }

  @Test
  @DisplayName("withServerOptions applies non-null server overrides")
  void withServerOptions_appliesOverrides() {
    var base =
        ConfigFactory.parseString(
            "larpconnect.server.port = 8080\nlarpconnect.server.host = \"0.0.0.0\"");
    var builder = new CliConfigBuilder(base);

    var config =
        builder
            .withServerOptions(
                new ServerOptions("127.0.0.1", 9090, "node-1", "test.org", "test@test.org"))
            .build();

    assertThat(config.getString("larpconnect.server.host")).isEqualTo("127.0.0.1");
    assertThat(config.getInt("larpconnect.server.port")).isEqualTo(9090);
    assertThat(config.getString("larpconnect.server.name")).isEqualTo("node-1");
    assertThat(config.getString("larpconnect.server.primary-domain")).isEqualTo("test.org");
    assertThat(config.getString("larpconnect.server.admin-contact")).isEqualTo("test@test.org");
  }

  @Test
  @DisplayName("withServerOptions with Optional components applies overrides")
  void withServerOptions_withOptionalComponents_appliesOverrides() {
    var base = ConfigFactory.parseString("larpconnect.server.port = 8080");
    var builder = new CliConfigBuilder(base);

    var options =
        new ServerOptions(
            Optional.of("127.0.0.1"),
            Optional.of(9090),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    var applied = builder.withServerOptions(options).build();
    assertThat(applied.getString("larpconnect.server.host")).isEqualTo("127.0.0.1");
    assertThat(applied.getInt("larpconnect.server.port")).isEqualTo(9090);
  }

  @Test
  @DisplayName("withServerOptions ignores null options safely")
  void withServerOptions_whenNull_returnsUnmodified() {
    var base = ConfigFactory.parseString("larpconnect.server.port = 8080");
    var builder = new CliConfigBuilder(base);

    var config = builder.withServerOptions(null).build();

    assertThat(config.getInt("larpconnect.server.port")).isEqualTo(8080);
  }

  @Test
  @DisplayName("withMigrationOptions applies non-null migration overrides")
  void withMigrationOptions_appliesOverrides() {
    var base =
        ConfigFactory.parseString(
            "larpconnect.data.database.migration.jdbc-url = \"jdbc:postgresql://localhost/db\"\n"
                + "larpconnect.data.database.migration.username = \"njall\"\n"
                + "larpconnect.data.database.migration.password = \"\"\n"
                + "larpconnect.data.database.migration.schemas = [\"njall\"]\n"
                + "larpconnect.data.database.migration.default-schema = \"njall\"");
    var builder = new CliConfigBuilder(base);

    var config =
        builder
            .withMigrationOptions(
                new MigrationOptions(
                    "jdbc:postgresql://remote:5432/custom",
                    "custom_user",
                    "secret",
                    List.of("schema1", "schema2"),
                    "schema1",
                    "custom-node",
                    "custom.domain",
                    "custom@domain.com"))
            .build();

    assertThat(config.getString("larpconnect.data.database.migration.jdbc-url"))
        .isEqualTo("jdbc:postgresql://remote:5432/custom");
    assertThat(config.getString("larpconnect.data.database.migration.username"))
        .isEqualTo("custom_user");
    assertThat(config.getString("larpconnect.data.database.migration.password"))
        .isEqualTo("secret");
    assertThat(config.getStringList("larpconnect.data.database.migration.schemas"))
        .containsExactly("schema1", "schema2");
    assertThat(config.getString("larpconnect.data.database.migration.default-schema"))
        .isEqualTo("schema1");
    assertThat(config.getString("larpconnect.server.name")).isEqualTo("custom-node");
  }

  @Test
  @DisplayName("withMigrationOptions with Optional components applies overrides")
  void withMigrationOptions_withOptionalComponents_appliesOverrides() {
    var base =
        ConfigFactory.parseString(
            "larpconnect.data.database.migration.username = \"default_user\"");
    var builder = new CliConfigBuilder(base);

    var options =
        new MigrationOptions(
            Optional.of("jdbc:postgresql://db/njall"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    var applied = builder.withMigrationOptions(options).build();
    assertThat(applied.getString("larpconnect.data.database.migration.jdbc-url"))
        .isEqualTo("jdbc:postgresql://db/njall");
    assertThat(applied.getString("larpconnect.data.database.migration.username"))
        .isEqualTo("default_user");
  }

  @Test
  @DisplayName("withMigrationOptions applies trustAuth override")
  void withMigrationOptions_appliesTrustAuthOverride() {
    var base = ConfigFactory.parseString("larpconnect.data.database.migration.trust-auth = false");
    var builder = new CliConfigBuilder(base);

    var options = new MigrationOptions(null, null, null, true, null, null, null, null, null);

    var applied = builder.withMigrationOptions(options).build();
    assertThat(applied.getBoolean("larpconnect.data.database.migration.trust-auth")).isTrue();
  }

  @Test
  @DisplayName("withMigrationOptions ignores null options safely")
  void withMigrationOptions_whenNull_returnsUnmodified() {
    var base =
        ConfigFactory.parseString("larpconnect.data.database.migration.username = \"njall\"");
    var builder = new CliConfigBuilder(base);

    var config = builder.withMigrationOptions(null).build();

    assertThat(config.getString("larpconnect.data.database.migration.username")).isEqualTo("njall");
  }

  @Test
  @DisplayName("withOverride ignores null or empty list values")
  void withOverride_ignoresNullAndEmptyList() {
    var base =
        ConfigFactory.parseString("larpconnect.server.port = 8080\nlarpconnect.list = [\"a\"]");
    var builder = new CliConfigBuilder(base);

    var config =
        builder
            .withOverride("larpconnect.server.port", null)
            .withOverride("larpconnect.list", Collections.emptyList())
            .build();

    assertThat(config.getInt("larpconnect.server.port")).isEqualTo(8080);
    assertThat(config.getStringList("larpconnect.list")).containsExactly("a");
  }

  @Test
  @DisplayName("withConfigFile layers external configuration file")
  void withConfigFile_appliesExternalFile() throws IOException {
    var tempFile = Files.createTempFile("larpconnect-test", ".conf");
    try {
      Files.writeString(tempFile, "larpconnect.server.port = 9999\n");
      var base =
          ConfigFactory.parseString(
              "larpconnect.server.port = 8080\nlarpconnect.server.host = \"0.0.0.0\"");
      var builder = new CliConfigBuilder(base).withConfigFile(tempFile.toFile());

      var config = builder.build();

      assertThat(config.getInt("larpconnect.server.port")).isEqualTo(9999);
      assertThat(config.getString("larpconnect.server.host")).isEqualTo("0.0.0.0");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @DisplayName("withConfigFile with Optional applies file or ignores empty")
  void withConfigFile_withOptional_appliesOrIgnoresEmpty() throws IOException {
    var tempFile = Files.createTempFile("larpconnect-opt-test", ".conf");
    try {
      Files.writeString(tempFile, "larpconnect.server.port = 7777\n");
      var base = ConfigFactory.parseString("larpconnect.server.port = 8080");

      var applied =
          new CliConfigBuilder(base).withConfigFile(Optional.of(tempFile.toFile())).build();
      assertThat(applied.getInt("larpconnect.server.port")).isEqualTo(7777);

      var emptyApplied =
          new CliConfigBuilder(base).withConfigFile(Optional.<java.io.File>empty()).build();
      assertThat(emptyApplied.getInt("larpconnect.server.port")).isEqualTo(8080);
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @DisplayName("withConfigFile throws IllegalArgumentException when file does not exist")
  void withConfigFile_nonExistentFile_throwsException() {
    var nonExistent = new java.io.File("non_existent_config_file.conf");
    var builder = new CliConfigBuilder().withConfigFile(nonExistent);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Specified configuration file does not exist");
  }
}
