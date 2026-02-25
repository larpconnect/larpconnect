package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public final class ServerModule extends AbstractModule {

  public ServerModule() {}

  @Override
  protected void configure() {
    bind(MainVerticle.class).to(DefaultMainVerticle.class).in(Scopes.SINGLETON);
  }
}
