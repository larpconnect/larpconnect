package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.api.ApiModule;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.init.InitModule;

public final class ServerModule extends AbstractModule {
  public ServerModule() {}

  @Override
  protected void configure() {
    install(new ApiModule());
    install(new CommonModule());
    install(new InitModule());
    install(new ServerBindingModule());
  }
}
