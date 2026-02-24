package com.larpconnect.njall.init;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleLifecycle extends AbstractIdleService implements VerticleService {
  private static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 120;
  private final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);

  private final List<Module> modules;
  private final Supplier<Vertx> vertxSupplier;
  private Injector injector;
  private Vertx vertx;
  private long shutdownTimeoutSeconds = DEFAULT_SHUTDOWN_TIMEOUT_SECONDS;

  VerticleLifecycle(List<Module> modules) {
    this(modules, Vertx::vertx);
  }

  // Visible for testing
  VerticleLifecycle(List<Module> modules, Supplier<Vertx> vertxSupplier) {
    this.modules = new ArrayList<>(modules);
    this.vertxSupplier = vertxSupplier;
  }

  // Visible for testing
  void setShutdownTimeoutSeconds(long seconds) {
    this.shutdownTimeoutSeconds = seconds;
  }

  @Override
  protected void startUp() throws Exception {
    logger.info("Starting VerticleLifecycle...");

    // Create Vertx instance
    vertx = vertxSupplier.get();

    // Add Vertx module to expose Vertx instance to Guice
    modules.add(
        new AbstractModule() {
          @Override
          protected void configure() {}

          @Provides
          @Singleton
          Vertx provideVertx() {
            return vertx;
          }
        });

    // Create Guice Injector
    injector = Guice.createInjector(modules);

    // Setup Verticle Factory via VerticleSetupService
    VerticleSetupService setupService = injector.getInstance(VerticleSetupService.class);
    setupService.setup(vertx, injector);

    logger.info("VerticleLifecycle started.");
  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping VerticleLifecycle...");
    if (vertx != null) {
      CountDownLatch latch = new CountDownLatch(1);
      vertx
          .close()
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  logger.info("Vert.x closed successfully.");
                } else {
                  logger.error("Failed to close Vert.x", ar.cause());
                }
                latch.countDown();
              });
      if (!latch.await(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
        logger.warn("Timed out waiting for Vert.x to close.");
      }
    }
  }

  @Override
  public Injector getInjector() {
    return injector;
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }
}
