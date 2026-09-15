package com.larpconnect.njall.server;

import static java.util.Objects.requireNonNull;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.server.http.HttpServerService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the Project Njall HTTP application. */
public final class ServerApp {

  private final Logger logger = LoggerFactory.getLogger(ServerApp.class);
  private final Supplier<Injector> injectorSupplier;
  private final Consumer<Injector> serverLauncher;
  private final Consumer<Thread> shutdownHookRegistrar;

  public ServerApp() {
    this(
        () -> Guice.createInjector(new ServerModule()),
        null,
        Runtime.getRuntime()::addShutdownHook);
  }

  public ServerApp(Supplier<Injector> injectorSupplier, Consumer<Injector> serverLauncher) {
    this(injectorSupplier, serverLauncher, Runtime.getRuntime()::addShutdownHook);
  }

  public ServerApp(
      Supplier<Injector> injectorSupplier,
      Consumer<Injector> serverLauncher,
      Consumer<Thread> shutdownHookRegistrar) {
    this.injectorSupplier = requireNonNull(injectorSupplier, "injectorSupplier cannot be null");
    this.serverLauncher = serverLauncher != null ? serverLauncher : this::defaultLaunch;
    this.shutdownHookRegistrar =
        requireNonNull(shutdownHookRegistrar, "shutdownHookRegistrar cannot be null");
  }

  public static void main(String[] args) {
    var app = new ServerApp();
    var exitCode = app.runWithArgs(args);
    if (exitCode != null) {
      System.exit(exitCode);
    }
  }

  public Integer runWithArgs(String[] args) {
    var cliArgs = parseArgs(args);
    if (cliArgs.migrate()) {
      return executeMigration();
    }
    run();
    return null;
  }

  CliArgs parseArgs(String[] args) {
    return CliArgs.parse(args);
  }

  int executeMigration() {
    var migrator = resolveMigrator();
    return performMigration(migrator);
  }

  private DatabaseMigrator resolveMigrator() {
    return createInjector().getInstance(DatabaseMigrator.class);
  }

  private int performMigration(DatabaseMigrator migrator) {
    logger.info("Starting database migration task from CLI flag...");
    try {
      migrator.migrate();
      logger.info("Database migration completed successfully.");
      return 0;
    } catch (Exception e) {
      logger.error("Database migration failed", e);
      return 1;
    }
  }

  void run() {
    var injector = createInjector();
    launchServer(injector);
    registerShutdownHook(injector);
  }

  private void launchServer(Injector injector) {
    logger.info("Bootstrapping LarpConnect server...");
    serverLauncher.accept(injector);
  }

  Injector createInjector() {
    return injectorSupplier.get();
  }

  private void defaultLaunch(Injector injector) {
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

  void registerShutdownHook(Injector injector) {
    try {
      var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));
      if (system != null) {
        var hook =
            new Thread(
                () -> {
                  logger.info("JVM shutdown initiated; invoking CoordinatedShutdown...");
                  try {
                    CoordinatedShutdown.get(system).run(CoordinatedShutdown.jvmExitReason());
                  } catch (Exception e) {
                    logger.warn("CoordinatedShutdown failed on JVM exit", e);
                  }
                },
                "pekko-coordinated-shutdown");
        shutdownHookRegistrar.accept(hook);
      }
    } catch (Exception e) {
      logger.warn("Could not register shutdown hook for ActorSystem", e);
    }
  }
}
