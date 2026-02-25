package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import jakarta.inject.Singleton;

final class VertxModule extends AbstractModule {
  private final VertxProvider vertxProvider;

  VertxModule(VertxProvider vertxProvider) {
    this.vertxProvider = vertxProvider;
  }

  @Override
  protected void configure() {
    bind(Vertx.class).toProvider(vertxProvider).in(Singleton.class);
  }
}
