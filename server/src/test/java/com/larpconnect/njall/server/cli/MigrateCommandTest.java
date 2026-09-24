package com.larpconnect.njall.server.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class MigrateCommandTest {

  @Test
  @DisplayName("migration options parse correctly")
  void parse_migrationOptions() {
    var migrateCommand = new MigrateCommand();
    var cmd = new CommandLine(migrateCommand);

    cmd.parseArgs(
        "--jdbc-url",
        "jdbc:postgresql://db:5432/larpconnect",
        "-u",
        "admin_user",
        "-p",
        "secret_pass",
        "--trust-auth",
        "--schemas",
        "s1,s2,s3",
        "--default-schema",
        "s1",
        "--server-name",
        "node-x",
        "--primary-domain",
        "custom.domain",
        "--admin-contact",
        "admin@domain.com");

    assertThat(migrateCommand.jdbcUrl()).contains("jdbc:postgresql://db:5432/larpconnect");
    assertThat(migrateCommand.username()).contains("admin_user");
    assertThat(migrateCommand.password()).contains("secret_pass");
    assertThat(migrateCommand.trustAuth()).contains(true);
    assertThat(migrateCommand.schemas()).contains(ImmutableList.of("s1", "s2", "s3"));
    assertThat(migrateCommand.defaultSchema()).contains("s1");
    assertThat(migrateCommand.serverName()).contains("node-x");
    assertThat(migrateCommand.primaryDomain()).contains("custom.domain");
    assertThat(migrateCommand.adminContact()).contains("admin@domain.com");
  }

  @Test
  @DisplayName("call executes migration successfully and returns executor exit code")
  void call_whenMigrateSucceeds_returnsZero() {
    var capturedConfig = new AtomicReference<Config>();
    var migrateCommand =
        new MigrateCommand(
            cfg -> {
              capturedConfig.set(cfg);
              return 0;
            });
    var cmd = new CommandLine(migrateCommand);
    cmd.parseArgs("--jdbc-url", "jdbc:postgresql://remote:5432/db");

    var exitCode = migrateCommand.call();

    assertThat(exitCode).isEqualTo(0);
    assertThat(capturedConfig.get()).isNotNull();
    assertThat(capturedConfig.get().getString("larpconnect.data.database.migration.jdbc-url"))
        .isEqualTo("jdbc:postgresql://remote:5432/db");
  }

  @Test
  @DisplayName("call passes trust-auth flag into migration configuration")
  void call_whenTrustAuthSpecified_setsTrustAuthOverrideInConfig() {
    var capturedConfig = new AtomicReference<Config>();
    var migrateCommand =
        new MigrateCommand(
            cfg -> {
              capturedConfig.set(cfg);
              return 0;
            });
    var cmd = new CommandLine(migrateCommand);
    cmd.parseArgs("--trust-auth");

    var exitCode = migrateCommand.call();

    assertThat(exitCode).isEqualTo(0);
    assertThat(capturedConfig.get()).isNotNull();
    assertThat(capturedConfig.get().getBoolean("larpconnect.data.database.migration.trust-auth"))
        .isTrue();
  }

  @Test
  @DisplayName("call executes migration when invoked via root command hierarchy")
  void call_whenInvokedViaRoot_resolvesRootConfigAndReturnsZero() throws Exception {
    var capturedConfig = new AtomicReference<Config>();
    var migrateCommand =
        new MigrateCommand(
            cfg -> {
              capturedConfig.set(cfg);
              return 0;
            });

    var tempFile = Files.createTempFile("migrate-test", ".conf");
    try {
      Files.writeString(tempFile, "larpconnect.data.database.migration.username = \"temp_user\"\n");

      var factory =
          new CommandLine.IFactory() {
            @Override
            // Safe unchecked cast mapping known command class to mock instance
            @SuppressWarnings("unchecked")
            public <K> K create(Class<K> cls) throws Exception {
              if (cls == MigrateCommand.class) {
                return (K) migrateCommand;
              }
              return CommandLine.defaultFactory().create(cls);
            }
          };

      var rootCommand = new RootCommand();
      var cmd = new CommandLine(rootCommand, factory);
      var exitCode = cmd.execute("-c", tempFile.toString(), "migrate");

      assertThat(exitCode).isEqualTo(0);
      assertThat(capturedConfig.get()).isNotNull();
      assertThat(capturedConfig.get().getString("larpconnect.data.database.migration.username"))
          .isEqualTo("temp_user");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @DisplayName("call returns error exit code when migration fails")
  void call_whenMigrateFails_returnsOne() {
    var migrateCommand = new MigrateCommand(cfg -> 1);
    var exitCode = migrateCommand.call();

    assertThat(exitCode).isEqualTo(1);
  }

  @Test
  @DisplayName("default constructor initializes valid MigrateCommand")
  void defaultConstructor_initializes() {
    var migrateCommand = new MigrateCommand();
    assertThat(migrateCommand).isNotNull();
    assertThat(migrateCommand.jdbcUrl()).isEmpty();
    assertThat(migrateCommand.username()).isEmpty();
    assertThat(migrateCommand.password()).isEmpty();
    assertThat(migrateCommand.trustAuth()).isEmpty();
    assertThat(migrateCommand.schemas()).isEmpty();
    assertThat(migrateCommand.defaultSchema()).isEmpty();
    assertThat(migrateCommand.serverName()).isEmpty();
    assertThat(migrateCommand.primaryDomain()).isEmpty();
    assertThat(migrateCommand.adminContact()).isEmpty();
    assertThat(migrateCommand.call()).isEqualTo(0);
  }
}
