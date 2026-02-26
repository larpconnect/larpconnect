package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.Verticle;

public final class ServerModule extends AbstractModule {

  public ServerModule() {}

  @Override
  protected void configure() {
    bind(MainVerticle.class).to(DefaultMainVerticle.class).in(Scopes.SINGLETON);
    bind(ResourceLoader.class)
        .toInstance(path -> getClass().getClassLoader().getResourceAsStream(path));
    Multibinder.newSetBinder(binder(), Verticle.class).addBinding().to(WebServerVerticle.class);
  }
}
