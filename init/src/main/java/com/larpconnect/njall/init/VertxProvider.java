package com.larpconnect.njall.init;

import com.google.errorprone.annotations.ThreadSafe;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@ThreadSafe
final class VertxProvider implements Provider<Vertx> {
  private final Logger logger = LoggerFactory.getLogger(VertxProvider.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();
  private final Supplier<Vertx> vertxFactory;

  VertxProvider() {
    this(Vertx::vertx);
  }

  // Visible for testing
  VertxProvider(Supplier<Vertx> vertxFactory) {
    this.vertxFactory = vertxFactory;
  }

  @Override
  public Vertx get() {
    return vertxRef.accumulateAndGet(
        null,
        (current, x) -> {
          if (current != null) {
            return current;
          }
          return vertxFactory.get();
        });
  }

  Future<Void> close() {
    var vertx = vertxRef.getAndSet(null);
    if (vertx != null) {
      return vertx
          .close()
          .onSuccess(v -> logger.info("Vert.x closed successfully."))
          .onFailure(err -> logger.error("Failed to close Vert.x", err));
    }
    return Future.succeededFuture();
  }
}
