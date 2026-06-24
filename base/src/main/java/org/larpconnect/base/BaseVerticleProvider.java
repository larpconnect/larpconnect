package org.larpconnect.base;

import com.google.inject.Inject;
import io.vertx.core.Verticle;
import org.larpconnect.events.VerticleProvider;

/** Provides the BaseVerticle instance for deployment. */
final class BaseVerticleProvider implements VerticleProvider {
  private final BaseVerticle verticle;

  @Inject
  BaseVerticleProvider(BaseVerticle verticle) {
    this.verticle = verticle;
  }

  @Override
  public Verticle getVerticle() {
    return verticle;
  }
}
