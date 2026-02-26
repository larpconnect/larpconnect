package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.server.grpc.GrpcModule;

public final class ServerModule extends AbstractModule {

  public ServerModule() {}

  @Override
  protected void configure() {
    install(new BaseServerModule());
    install(new GrpcModule());
  }
}
