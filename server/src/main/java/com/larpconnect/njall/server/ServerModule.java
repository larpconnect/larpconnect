package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.CommonModule;

public final class ServerModule extends AbstractModule {
  public ServerModule() {}

  @Override
  protected void configure() {
    install(new CommonModule());
    install(new ServerBindingModule());
  }
}
