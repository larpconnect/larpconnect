package com.larpconnect.njall.api.verticle;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.Verticle;

/**
 * Guice bindings for the API verticles. Placed in the same package as the package-private verticles
 * to bypass visibility issues.
 */
public final class ApiVerticleModule extends AbstractModule {
  /**
   * Constructs a new {@link ApiVerticleModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation while
   * encapsulating internal package-private bindings.
   */
  public ApiVerticleModule() {}

  @Override
  protected void configure() {
    Multibinder<Verticle> verticleBinder = Multibinder.newSetBinder(binder(), Verticle.class);
    verticleBinder.addBinding().to(WebfingerVerticle.class);
    verticleBinder.addBinding().to(NodeinfoWellKnownVerticle.class);
    verticleBinder.addBinding().to(NodeinfoVerticle.class);
  }
}
