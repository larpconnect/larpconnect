package org.larpconnect.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the LarpConnect application. */
public final class ServerApp {
  private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  private static volatile ServerService runningService;
  private static volatile Module overrideModule;

  private ServerApp() {}

  /**
   * Sets a module to override the default ServerModule bindings during testing.
   *
   * @param module The overriding Guice module.
   */
  static void setOverrideModule(Module module) {
    overrideModule = module;
  }

  /**
   * Returns the currently running ServerService instance, if any.
   *
   * @return The running ServerService or null.
   */
  static ServerService getRunningService() {
    return runningService;
  }

  /**
   * Main execution method.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    try {
      logger.info("Starting LarpConnect Server...");

      Module module = new ServerModule();
      if (overrideModule != null) {
        module = Modules.override(module).with(overrideModule);
      }

      Injector injector = Guice.createInjector(module);
      logger.info("Injector configured successfully.");

      ServerService service = injector.getInstance(ServerService.class);
      runningService = service;

      Runtime.getRuntime()
          .addShutdownHook(new Thread(createShutdownHookRunnable(service), "shutdown-hook"));

      logger.info("Starting ServerService...");
      service.startAsync().awaitRunning();
      logger.info("LarpConnect Server is running.");
    } catch (Exception e) {
      logger.error("Fatal error starting ServerApp", e);
      System.exit(1);
    }
  }

  static Runnable createShutdownHookRunnable(ServerService service) {
    return () -> {
      logger.info("Shutdown hook triggered. Stopping ServerService...");
      try {
        service.stopAsync().awaitTerminated();
        logger.info("ServerService terminated successfully.");
      } catch (Exception e) {
        logger.error("Error during server shutdown", e);
      }
    };
  }
}
