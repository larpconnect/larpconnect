package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;

public final class ServerModule extends AbstractModule {

  public ServerModule() {}

  @Override
  protected void configure() {
    install(new com.larpconnect.njall.common.CommonModule());
    install(new ServerBindingModule());
    install(new ServerProvidesModule());
  }
}
