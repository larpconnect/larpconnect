package com.larpconnect.njall.server;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Main {
  private final Logger logger = LoggerFactory.getLogger(Main.class);
  private static final Duration SHUTDOWN_TIMEOUT = Duration.ofMinutes(2);

  private final Runtime runtime;
  private final Module serverModule;

  Main(Runtime runtime) {
    this(runtime, new ServerModule());
  }

  Main(Runtime runtime, Module serverModule) {
    this.runtime = runtime;
    this.serverModule = serverModule;
  }

  @AiContract(implementationHint = "Entry point")
  public static void main(String[] args) {
    new Main(Runtime.getRuntime()).run();
  }

  @AiContract(
      ensure = {"$ \\neq \\bot"},
      implementationHint = "Bootstraps Guice and deploys MainVerticle")
  VerticleService run() {
    logger.info("Starting Server...");

    // Register ServerModule to bind MainVerticle -> DefaultMainVerticle
    var lifecycle = VerticleServices.create(ImmutableList.of(serverModule));

    runtime.addShutdownHook(new Thread(() -> shutdown(lifecycle)));

    try {
      lifecycle.startAsync().awaitRunning();
      lifecycle.deploy(MainVerticle.class);
    } catch (RuntimeException e) {
      logger.error("Failed to start server", e);
      System.exit(1);
      return null;
    }
    return lifecycle;
  }

  @AiContract(
      require = {"lifecycle \\neq \\bot"},
      implementationHint = "Gracefully stops the verticle lifecycle")
  void shutdown(VerticleService lifecycle) {
    logger.info("Shutdown hook triggered...");
    try {
      lifecycle.stopAsync().awaitTerminated(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
      logger.info("Server shutdown successfully.");
    } catch (TimeoutException e) {
      logger.error("Shutdown timed out", e);
    } catch (RuntimeException e) {
      logger.error("Error during shutdown", e);
    }
  }
}
