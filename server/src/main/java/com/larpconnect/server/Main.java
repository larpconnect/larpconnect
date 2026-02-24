package com.larpconnect.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the server application. */
final class Main {
  private final Logger logger = LoggerFactory.getLogger(Main.class);

  private Main() {}

  /**
   * Main method.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    new Main().logger.info("Starting server...");
  }
}
