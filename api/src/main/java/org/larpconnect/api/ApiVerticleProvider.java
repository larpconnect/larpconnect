package org.larpconnect.api;

import com.google.inject.Inject;
import io.vertx.core.Verticle;
import org.larpconnect.events.VerticleProvider;

/** Provides the ApiVerticle instance for deployment. */
final class ApiVerticleProvider implements VerticleProvider {
  private final ApiVerticle verticle;

  @Inject
  ApiVerticleProvider(ApiVerticle verticle) {
    this.verticle = verticle;
  }

  @Override
  public Verticle getVerticle() {
    return verticle;
  }
}
