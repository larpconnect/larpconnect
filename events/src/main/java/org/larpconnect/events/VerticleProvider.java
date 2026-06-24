package org.larpconnect.events;

import io.vertx.core.Verticle;

/** Provider interface for dynamically registering Vert.x Verticles. */
public interface VerticleProvider {
  /** Returns the Verticle instance to be deployed. */
  Verticle getVerticle();
}
