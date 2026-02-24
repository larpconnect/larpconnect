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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleLifecycle extends AbstractIdleService implements VerticleService {
  private final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);

  private final List<Module> modules;
  private Injector injector;
  private Vertx vertx;

  VerticleLifecycle(List<Module> modules) {
    this.modules = new ArrayList<>(modules);
  }

  @Override
  protected void startUp() throws Exception {
    logger.info("Starting VerticleLifecycle...");

    // Create Vertx instance
    vertx = Vertx.vertx();

    // Add Vertx module to expose Vertx instance to Guice
    modules.add(new AbstractModule() {
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
      vertx.close().onComplete(ar -> {
        if (ar.succeeded()) {
          logger.info("Vert.x closed successfully.");
        } else {
          logger.error("Failed to close Vert.x", ar.cause());
        }
        latch.countDown();
      });
      if (!latch.await(2, TimeUnit.MINUTES)) {
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
