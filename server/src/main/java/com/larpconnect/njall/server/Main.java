package com.larpconnect.njall.server;

import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Main {
  private final Logger logger = LoggerFactory.getLogger(Main.class);

  Main() {}

  public static void main(String[] args) {
    new Main().run();
  }

  VerticleService run() {
    logger.info("Starting Server...");

    // Register ServerModule to bind ServerVerticle -> MainVerticle
    var lifecycle = VerticleServices.create(Collections.singletonList(new ServerModule()));

    Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(lifecycle)));

    try {
      lifecycle.startAsync().awaitRunning();
      lifecycle.deploy(ServerVerticle.class);
    } catch (RuntimeException e) {
      logger.error("Failed to start server", e);
      System.exit(1);
      return null;
    }
    return lifecycle;
  }

  void shutdown(VerticleService lifecycle) {
    logger.info("Shutdown hook triggered...");
    try {
      lifecycle.stopAsync().awaitTerminated(2, TimeUnit.MINUTES);
    } catch (TimeoutException e) {
      logger.error("Shutdown timed out", e);
    } catch (RuntimeException e) {
      logger.error("Error during shutdown", e);
    }
  }
}
