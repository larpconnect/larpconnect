package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.Verticle;

final class BaseServerModule extends AbstractModule {

  BaseServerModule() {}

  @Override
  protected void configure() {
    bind(MainVerticle.class).to(DefaultMainVerticle.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), Verticle.class);
  }
}
