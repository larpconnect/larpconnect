package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public final class ServerModule extends AbstractModule {

  public ServerModule() {}

  @Override
  protected void configure() {
    bind(ServerVerticle.class).to(MainVerticle.class).in(Scopes.SINGLETON);
  }
}
