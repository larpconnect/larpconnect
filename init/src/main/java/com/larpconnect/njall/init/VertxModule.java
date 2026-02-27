package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Vertx;
import jakarta.inject.Singleton;

final class VertxModule extends AbstractModule {
  private final VertxProvider vertxProvider;

  VertxModule(VertxProvider vertxProvider) {
    this.vertxProvider = vertxProvider;
  }

  @Override
  @AiContract(
      ensure = "Vertx \\text{ is bound to } VertxProvider \\text{ in } Singleton",
      implementationHint = "Binds Vertx to the provider as a singleton.")
  protected void configure() {
    bind(Vertx.class).toProvider(vertxProvider).in(Singleton.class);
  }
}
