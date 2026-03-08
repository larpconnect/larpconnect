package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import jakarta.inject.Singleton;

final class VertxModule extends AbstractModule {
  private final VerticleService verticleService;

  VertxModule(VerticleService verticleService) {
    this.verticleService = verticleService;
  }

  @Override
  protected void configure() {
    bind(VerticleDeployer.class).to(VerticleService.class);
    bind(VerticleService.class).toInstance(verticleService);
    bind(Vertx.class).toProvider(VertxProvider.class).in(Singleton.class);
  }
}
