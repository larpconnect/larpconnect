package org.larpconnect.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the LarpConnect application. */
public final class ServerApp {
  private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  /**
   * Main execution method.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    logger.info("Starting LarpConnect Server...");
    Injector injector = Guice.createInjector(new ServerModule());
    logger.info("Injector configured successfully: {}", injector);
  }
}
