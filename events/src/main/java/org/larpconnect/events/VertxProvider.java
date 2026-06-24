package org.larpconnect.events;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.vertx.core.Vertx;

/** Provides a thread-safe Singleton Vert.x instance. */
public final class VertxProvider implements Provider<Vertx> {
  private final Provider<GuiceVerticleFactory> factoryProvider;
  private volatile Vertx vertx;

  @Inject
  public VertxProvider(Provider<GuiceVerticleFactory> factoryProvider) {
    this.factoryProvider = factoryProvider;
  }

  @Override
  public Vertx get() {
    Vertx localRef = vertx;
    if (localRef == null) {
      synchronized (this) {
        localRef = vertx;
        if (localRef == null) {
          localRef = Vertx.vertx();
          localRef.registerVerticleFactory(factoryProvider.get());
          vertx = localRef;
        }
      }
    }
    return localRef;
  }
}
