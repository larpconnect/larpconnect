package com.larpconnect.njall.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.server.http.HttpServerService;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the Project Njall HTTP application. */
public final class ServerApp {

  private final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  private ServerApp() {}

  public static void main(String[] args) {
    new ServerApp().run();
  }

  void run() {
    logger.info("Bootstrapping LarpConnect server...");
    var injector = createInjector();
    var server = injector.getInstance(HttpServerService.class);
    server
        .start()
        .whenComplete(
            (binding, throwable) -> {
              if (throwable != null) {
                logger.error("Failed to bind server socket", throwable);
              }
            });
    registerShutdownHook(injector);
  }

  Injector createInjector() {
    return Guice.createInjector(new ServerModule());
  }

  void registerShutdownHook(Injector injector) {
    var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("JVM shutdown initiated; invoking CoordinatedShutdown...");
                  CoordinatedShutdown.get(system).run(CoordinatedShutdown.jvmExitReason());
                }));
  }
}
