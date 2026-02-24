package com.larpconnect.njall.server;

import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import io.vertx.core.Vertx;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Main {
  private final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    new Main().run();
  }

  private void run() {
    logger.info("Starting Server...");

    VerticleService lifecycle = VerticleServices.create(Collections.emptyList());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook triggered...");
      try {
        lifecycle.stopAsync().awaitTerminated(2, TimeUnit.MINUTES);
      } catch (TimeoutException e) {
        logger.error("Shutdown timed out", e);
      } catch (Exception e) {
        logger.error("Error during shutdown", e);
      }
    }));

    try {
      lifecycle.startAsync().awaitRunning();

      Vertx vertx = lifecycle.getVertx();

      vertx.deployVerticle("guice:" + MainVerticle.class.getName())
        .onSuccess(id -> logger.info("MainVerticle deployed successfully with ID: {}", id))
        .onFailure(err -> {
           logger.error("Failed to deploy MainVerticle", err);
           lifecycle.stopAsync();
           System.exit(1);
        });

    } catch (Exception e) {
      logger.error("Failed to start server", e);
      System.exit(1);
    }
  }
}
