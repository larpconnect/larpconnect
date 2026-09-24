package com.larpconnect.njall.server.cli;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/** Subcommand for running Flyway schema migrations and seed provisioning. */
@Command(
    name = "migrate",
    description = "Executes Flyway database schema and seed migrations.",
    mixinStandardHelpOptions = true)
public final class MigrateCommand implements Callable<Integer> {

  private final Logger logger = LoggerFactory.getLogger(MigrateCommand.class);

  @ParentCommand private @Nullable RootCommand rootCommand;

  @Option(
      names = {"--jdbc-url"},
      description = "JDBC database connection URL.")
  private @Nullable String jdbcUrl;

  @Option(
      names = {"-u", "--username"},
      description = "Administrative database user name.")
  private @Nullable String username;

  @Option(
      names = {"-p", "--password"},
      description = "Administrative database password.")
  private @Nullable String password;

  @Option(
      names = {"--trust-auth"},
      description = "Explicitly enable passwordless trust authentication.")
  private @Nullable Boolean trustAuth;

  @Option(
      names = {"--schemas"},
      split = ",",
      description = "Comma-separated list of managed database schemas.")
  private @Nullable List<String> schemas;

  @Option(
      names = {"--default-schema"},
      description = "Default schema for Flyway schema history table.")
  private @Nullable String defaultSchema;

  @Option(
      names = {"--server-name"},
      description = "Server name for database seed placeholder substitution.")
  private @Nullable String serverName;

  @Option(
      names = {"--primary-domain"},
      description = "Primary domain for database seed placeholder substitution.")
  private @Nullable String primaryDomain;

  @Option(
      names = {"--admin-contact"},
      description = "Admin contact email for database seed placeholder substitution.")
  private @Nullable String adminContact;

  private final Function<Config, Integer> migrationExecutor;

  public MigrateCommand() {
    this(config -> 0);
  }

  public MigrateCommand(Function<Config, Integer> migrationExecutor) {
    this.migrationExecutor = migrationExecutor;
  }

  public Optional<String> jdbcUrl() {
    return Optional.ofNullable(jdbcUrl);
  }

  public Optional<String> username() {
    return Optional.ofNullable(username);
  }

  public Optional<String> password() {
    return Optional.ofNullable(password);
  }

  public Optional<Boolean> trustAuth() {
    return Optional.ofNullable(trustAuth);
  }

  public Optional<ImmutableList<String>> schemas() {
    return Optional.ofNullable(schemas).map(ImmutableList::copyOf);
  }

  public Optional<String> defaultSchema() {
    return Optional.ofNullable(defaultSchema);
  }

  public Optional<String> serverName() {
    return Optional.ofNullable(serverName);
  }

  public Optional<String> primaryDomain() {
    return Optional.ofNullable(primaryDomain);
  }

  public Optional<String> adminContact() {
    return Optional.ofNullable(adminContact);
  }

  @Override
  public Integer call() {
    logMigrationStart();
    var configFile = resolveConfigFile();
    var config = buildConfig(configFile);
    return executeMigration(config);
  }

  private void logMigrationStart() {
    logger.info("Executing database migration task from CLI command...");
  }

  private Integer executeMigration(Config config) {
    return migrationExecutor.apply(config);
  }

  private Optional<File> resolveConfigFile() {
    return Optional.ofNullable(rootCommand).flatMap(RootCommand::configFile);
  }

  private Config buildConfig(Optional<File> configFile) {
    var builder = createConfigBuilder();
    var options = createMigrationOptions();
    return builder.withConfigFile(configFile).withMigrationOptions(options).build();
  }

  private CliConfigBuilder createConfigBuilder() {
    return new CliConfigBuilder();
  }

  private MigrationOptions createMigrationOptions() {
    return new MigrationOptions(
        jdbcUrl(),
        username(),
        password(),
        trustAuth(),
        schemas(),
        defaultSchema(),
        serverName(),
        primaryDomain(),
        adminContact());
  }
}
