package com.larpconnect.njall.init;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
final class VerticleLifecycle extends AbstractIdleService implements VerticleService {
  private final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);

  private final Provider<Vertx> vertxProvider;
  private final VerticleSetupService setupService;

  @Inject
  VerticleLifecycle(
      Provider<Vertx> vertxProvider, VerticleSetupService setupService, Injector injector) {
    this.vertxProvider = vertxProvider;
    this.setupService = setupService;
    this.setupService.setup(vertxProvider.get(), injector);
  }

  @Override
  @AiContract(implementationHint = "Starts the lifecycle.")
  protected void startUp() {
    logger.info("VerticleLifecycle started.");
  }

  @Override
  @AiContract(implementationHint = "Gracefully shuts down the Vert.x instance.")
  protected void shutDown() {
    logger.info("Stopping VerticleLifecycle...");
    var latch = new CountDownLatch(1);
    vertxProvider
        .get()
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

  @Override
  @AiContract(
      require = "verticleClass \\neq \\bot",
      ensure = "verticleClass \\text{ is deployed via setupService}",
      implementationHint = "Delegates deployment to the setup service.")
  public void deploy(Class<? extends Verticle> verticleClass) {
    if (!isRunning()) {
      throw new IllegalStateException("VerticleLifecycle not started");
    }
    setupService.deploy(verticleClass);
  }
}
