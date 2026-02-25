package com.larpconnect.njall.init;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
final class VertxProvider implements Provider<Vertx> {
  private final Logger logger = LoggerFactory.getLogger(VertxProvider.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();
  private final Supplier<Vertx> vertxFactory;
  private final long closeTimeout;
  private final TimeUnit closeUnit;

  VertxProvider() {
    this(Vertx::vertx, 2, TimeUnit.MINUTES);
  }

  // Visible for testing
  VertxProvider(Supplier<Vertx> vertxFactory) {
    this(vertxFactory, 2, TimeUnit.MINUTES);
  }

  VertxProvider(Supplier<Vertx> vertxFactory, long closeTimeout, TimeUnit closeUnit) {
    this.vertxFactory = vertxFactory;
    this.closeTimeout = closeTimeout;
    this.closeUnit = closeUnit;
  }

  @Override
  public Vertx get() {
    Vertx vertx = vertxRef.get();
    if (vertx == null) {
      vertx = vertxFactory.get();
      if (!vertxRef.compareAndSet(null, vertx)) {
        vertx.close();
        return vertxRef.get();
      }
    }
    return vertx;
  }

  void close() {
    Vertx vertx = vertxRef.getAndSet(null);
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
      try {
        if (!latch.await(closeTimeout, closeUnit)) {
          logger.warn("Timed out waiting for Vert.x to close.");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Interrupted while waiting for Vert.x to close.");
      }
    }
  }
}
