package com.larpconnect.init;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;

/** Guice module for internal bindings. */
public final class InitModule extends AbstractModule {
  public InitModule() {}

  @Override
  protected void configure() {
    bind(Vertx.class).toProvider(VertxProvider.class);
    bind(ProtoCodecRegistry.class).to(DefaultProtoCodecRegistry.class);
    bind(VerticleSetupService.class).to(DefaultVerticleSetupService.class);
    bind(VerticleFactory.class).to(GuiceVerticleFactory.class);
  }
}
