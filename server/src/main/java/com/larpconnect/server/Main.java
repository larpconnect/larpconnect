package com.larpconnect.server;

import com.larpconnect.init.VerticleLifecycle;
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
    VerticleLifecycle lifecycle = new VerticleLifecycle();
    lifecycle.start();
    lifecycle.deployVerticle("guice:" + MainVerticle.class.getName());
  }
}
