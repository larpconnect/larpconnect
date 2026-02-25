package com.larpconnect.njall.init;

import com.google.errorprone.annotations.ThreadSafe;
import io.vertx.core.Vertx;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Singleton
@ThreadSafe
final class VertxProvider implements Provider<Vertx> {
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
    var vertx = vertxRef.get();
    if (vertx == null) {
      vertx = vertxFactory.get();
      if (!vertxRef.compareAndSet(null, vertx)) {
        vertx.close();
        return vertxRef.get();
      }
    }
    return vertx;
  }
}
