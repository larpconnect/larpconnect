package com.larpconnect.njall.server.cli;

import static java.util.Objects.requireNonNull;

import com.typesafe.config.Config;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
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

  @ParentCommand private RootCommand rootCommand;

  @Option(
      names = {"--jdbc-url"},
      description = "JDBC database connection URL.")
  private String jdbcUrl;

  @Option(
      names = {"-u", "--username"},
      description = "Administrative database user name.")
  private String username;

  @Option(
      names = {"-p", "--password"},
      description = "Administrative database password.")
  private String password;

  @Option(
      names = {"--schemas"},
      split = ",",
      description = "Comma-separated list of managed database schemas.")
  private List<String> schemas;

  @Option(
      names = {"--default-schema"},
      description = "Default schema for Flyway schema history table.")
  private String defaultSchema;

  @Option(
      names = {"--server-name"},
      description = "Server name for database seed placeholder substitution.")
  private String serverName;

  @Option(
      names = {"--primary-domain"},
      description = "Primary domain for database seed placeholder substitution.")
  private String primaryDomain;

  @Option(
      names = {"--admin-contact"},
      description = "Admin contact email for database seed placeholder substitution.")
  private String adminContact;

  private final Function<Config, Integer> migrationExecutor;

  public MigrateCommand() {
    this(config -> 0);
  }

  public MigrateCommand(Function<Config, Integer> migrationExecutor) {
    this.migrationExecutor = requireNonNull(migrationExecutor, "migrationExecutor cannot be null");
  }

  public String jdbcUrl() {
    return jdbcUrl;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public List<String> schemas() {
    return schemas;
  }

  public String defaultSchema() {
    return defaultSchema;
  }

  public String serverName() {
    return serverName;
  }

  public String primaryDomain() {
    return primaryDomain;
  }

  public String adminContact() {
    return adminContact;
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

  private File resolveConfigFile() {
    return rootCommand != null ? rootCommand.configFile() : null;
  }

  private Config buildConfig(File configFile) {
    var builder = createConfigBuilder();
    var options = createMigrationOptions();
    return builder.withConfigFile(configFile).withMigrationOptions(options).build();
  }

  private CliConfigBuilder createConfigBuilder() {
    return new CliConfigBuilder();
  }

  private MigrationOptions createMigrationOptions() {
    return new MigrationOptions(
        jdbcUrl,
        username,
        password,
        schemas,
        defaultSchema,
        serverName,
        primaryDomain,
        adminContact);
  }
}
