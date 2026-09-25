package com.larpconnect.njall.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.server.cli.CliRunner;
import com.typesafe.config.Config;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the Project Njall HTTP application and CLI management. */
public final class ServerApp {

  private final Logger logger = LoggerFactory.getLogger(ServerApp.class);
  private final CliRunner cliRunner;

  public ServerApp() {
    this.cliRunner = createDefaultCliRunner();
  }

  public ServerApp(CliRunner cliRunner) {
    this.cliRunner = cliRunner;
  }

  public static void main(String[] args) {
    var app = createApp();
    var exitCode = app.runWithArgs(args);
    terminateIfRequired(exitCode);
  }

  private static ServerApp createApp() {
    return new ServerApp();
  }

  private static void terminateIfRequired(@Nullable Integer exitCode) {
    if (exitCode != null) {
      System.exit(exitCode);
    }
  }

  public @Nullable Integer runWithArgs(String @Nullable [] args) {
    return cliRunner.execute(args);
  }

  CliRunner createDefaultCliRunner() {
    return new CliRunner(this::launchServer, this::executeMigration);
  }

  void launchServer(Config config) {
    var injector = createInjector(config);
    var service = resolveServerManager(injector);
    startServer(service);
  }

  private ServerManagerService resolveServerManager(Injector injector) {
    return injector.getInstance(ServerManagerService.class);
  }

  private void startServer(ServerManagerService service) {
    service.startAsync().awaitRunning();
  }

  int executeMigration(Config config) {
    var injector = createInjector(config);
    var migrator = resolveMigrator(injector);
    return runMigration(migrator);
  }

  private DatabaseMigrator resolveMigrator(Injector injector) {
    return injector.getInstance(DatabaseMigrator.class);
  }

  Injector createInjector(Config config) {
    var module = createServerModule(config);
    return buildInjector(module);
  }

  private ServerModule createServerModule(Config config) {
    return new ServerModule(config);
  }

  private Injector buildInjector(ServerModule module) {
    return Guice.createInjector(module);
  }

  private int runMigration(DatabaseMigrator migrator) {
    logger.info("Starting database migration task from CLI command...");
    try {
      migrator.migrate();
      logger.info("Database migration completed successfully.");
      return 0;
    } catch (Exception e) {
      logger.error("Database migration failed", e);
      return 1;
    }
  }
}
