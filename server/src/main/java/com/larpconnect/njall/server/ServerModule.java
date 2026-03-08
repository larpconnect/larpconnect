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
    // VertxModule and ConfigModule require state initialized dynamically during
    // VerticleLifecycle startup, so they are injected directly by the lifecycle
    // instead of statically installed here.
    install(new InitModule());
    install(new ServerBindingModule());
  }
}
