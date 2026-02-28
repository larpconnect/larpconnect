package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import jakarta.inject.Singleton;

final class VertxModule extends AbstractModule {
  VertxModule() {}

  @Override
  protected void configure() {
    bind(Vertx.class).toProvider(VertxProvider.class).in(Singleton.class);
  }
}
