package com.larpconnect.njall.server;

import static java.util.Objects.requireNonNull;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.server.cli.CliRunner;
import com.larpconnect.njall.server.http.HttpServerService;
import com.typesafe.config.Config;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
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
    this.cliRunner = requireNonNull(cliRunner, "cliRunner cannot be null");
  }

  public static void main(String[] args) {
    var app = createApp();
    var exitCode = app.runWithArgs(args);
    terminateIfRequired(exitCode);
  }

  private static ServerApp createApp() {
    return new ServerApp();
  }

  private static void terminateIfRequired(Integer exitCode) {
    if (exitCode != null) {
      System.exit(exitCode);
    }
  }

  public Integer runWithArgs(String[] args) {
    return cliRunner.execute(args);
  }

  CliRunner createDefaultCliRunner() {
    return new CliRunner(this::launchServer, this::executeMigration);
  }

  void launchServer(Config config) {
    var injector = createInjector(config);
    startHttpServer(injector);
    registerShutdownHook(injector, Runtime.getRuntime()::addShutdownHook);
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

  private void startHttpServer(Injector injector) {
    var server = injector.getInstance(HttpServerService.class);
    server
        .start()
        .whenComplete(
            (binding, throwable) -> {
              if (throwable != null) {
                logger.error("Failed to bind server socket", throwable);
              }
            });
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

  void registerShutdownHook(Injector injector, Consumer<Thread> hookRegistrar) {
    try {
      var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));
      if (system != null) {
        var hook = createShutdownThread(system);
        hookRegistrar.accept(hook);
      }
    } catch (Exception e) {
      logger.warn("Could not register shutdown hook for ActorSystem", e);
    }
  }

  private Thread createShutdownThread(ActorSystem<Void> system) {
    return new Thread(
        () -> {
          logger.info("JVM shutdown initiated; invoking CoordinatedShutdown...");
          try {
            CoordinatedShutdown.get(system)
                .runAll(CoordinatedShutdown.jvmExitReason())
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
          } catch (Exception e) {
            logger.warn("CoordinatedShutdown failed on JVM exit", e);
          }
        },
        "pekko-coordinated-shutdown");
  }
}
