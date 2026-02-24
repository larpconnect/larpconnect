package com.larpconnect.init;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a singleton {@link Vertx} instance. */
@Singleton
public final class VertxProvider implements Provider<Vertx> {
  private static final Logger logger = LoggerFactory.getLogger(VertxProvider.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();

  public VertxProvider() {}

  @Override
  public Vertx get() {
    Vertx vertx = vertxRef.get();
    if (vertx == null) {
      logger.info("Initializing Vert.x instance");
      vertx = Vertx.vertx();
      if (!vertxRef.compareAndSet(null, vertx)) {
        vertx.close();
        vertx = vertxRef.get();
      }
    }
    return vertx;
  }

  /**
   * Returns the Vert.x instance if it has been created, or null otherwise.
   *
   * @return the Vert.x instance or null
   */
  public Vertx getIfCreated() {
    return vertxRef.get();
  }
}
