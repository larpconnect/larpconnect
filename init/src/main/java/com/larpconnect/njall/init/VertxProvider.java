package com.larpconnect.njall.init;

import com.google.errorprone.annotations.ThreadSafe;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Singleton
@ThreadSafe
final class VertxProvider implements Provider<Vertx> {
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();
  private final Supplier<Vertx> vertxFactory;

  @Inject
  VertxProvider(JsonObject config) {
    this(() -> Vertx.vertx(new VertxOptions(config)));
  }

  // Visible for testing
  VertxProvider(Supplier<Vertx> vertxFactory) {
    this.vertxFactory = vertxFactory;
  }

  @Override
  @AiContract(
      ensure = {"$res \\neq \\bot$", "$res \\equiv vertxRef.get()$"},
      invariants = {"vertxRef \\text{ is initialized exactly once}"},
      implementationHint = "Lazily initializes and returns the singleton Vertx instance.")
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
