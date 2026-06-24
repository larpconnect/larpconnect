package org.larpconnect.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.Vertx;
import org.larpconnect.events.MainVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the LarpConnect application. */
public final class ServerApp {
  private final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  private ServerApp() {}

  /**
   * Main execution method.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    new ServerApp().run();
  }

  private void run() {
    logger.info("Starting LarpConnect Server...");
    Injector injector = Guice.createInjector(new ServerModule());
    logger.info("Injector configured successfully.");

    Vertx vertx = Vertx.vertx();
    MainVerticle mainVerticle = injector.getInstance(MainVerticle.class);
    vertx
        .deployVerticle(mainVerticle)
        .onSuccess(id -> logger.info("MainVerticle deployed successfully with ID: {}", id))
        .onFailure(
            err -> {
              logger.error("Failed to deploy MainVerticle", err);
              vertx.close();
            });
  }
}
