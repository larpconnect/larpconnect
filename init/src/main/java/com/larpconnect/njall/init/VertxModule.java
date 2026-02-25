package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;

final class VertxModule extends AbstractModule {
  private final VertxProvider vertxProvider;

  VertxModule(VertxProvider vertxProvider) {
    this.vertxProvider = vertxProvider;
  }

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  Vertx provideVertx() {
    return vertxProvider.get();
  }
}
