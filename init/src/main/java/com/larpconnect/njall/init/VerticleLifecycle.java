package com.larpconnect.njall.init;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Module;
import io.vertx.core.Verticle;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleLifecycle extends AbstractIdleService implements VerticleService {
  private final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);

  private final ImmutableList<Module> modules;
  private final VertxProvider vertxProvider;
  private final AtomicReference<VerticleSetupService> setupServiceRef = new AtomicReference<>();

  VerticleLifecycle(List<Module> modules) {
    this(modules, new VertxProvider());
  }

  // Visible for testing
  VerticleLifecycle(List<Module> modules, VertxProvider vertxProvider) {
    this.modules = ImmutableList.copyOf(modules);
    this.vertxProvider = vertxProvider;
  }

  @Override
  protected void startUp() {
    logger.info("Starting VerticleLifecycle...");

    // Create a mutable list to add our internal module
    var builder = ImmutableList.<Module>builder();
    builder.addAll(modules);
    builder.add(new VertxModule(vertxProvider));

    // Create Guice Injector
    var injector = Guice.createInjector(builder.build());

    // Setup Verticle Factory via VerticleSetupService
    var setupService = injector.getInstance(VerticleSetupService.class);
    setupService.setup(vertxProvider.get(), injector);
    setupServiceRef.set(setupService);

    logger.info("VerticleLifecycle started.");
  }

  @Override
  protected void shutDown() {
    logger.info("Stopping VerticleLifecycle...");
    var vertx = vertxProvider.release();
    if (vertx != null) {
      var latch = new CountDownLatch(1);
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
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Interrupted while waiting for Vert.x to close.");
      }
    }
  }

  @Override
  public void deploy(Class<? extends Verticle> verticleClass) {
    var service = setupServiceRef.get();
    if (service != null) {
      service.deploy(verticleClass);
    } else {
      throw new IllegalStateException("VerticleLifecycle not started");
    }
  }
}
