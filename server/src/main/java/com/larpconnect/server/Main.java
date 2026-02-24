package com.larpconnect.server;

import com.larpconnect.init.VerticleLifecycle;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the server application. */
public final class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private Main() {}

  /**
   * Main method.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    logger.info("Starting server...");
    VerticleLifecycle lifecycle = VerticleLifecycle.create();
    lifecycle.startAsync().awaitRunning();
    lifecycle.deployVerticle("guice:" + MainVerticle.class.getName());

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    lifecycle.stopAsync().awaitTerminated(2, TimeUnit.MINUTES);
                  } catch (java.util.concurrent.TimeoutException e) {
                    logger.error("Lifecycle stop timed out", e);
                  }
                }));
  }
}
