package com.larpconnect.njall.server.grpc;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.Verticle;

public final class GrpcModule extends AbstractModule {

  public GrpcModule() {}

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), Verticle.class).addBinding().to(GrpcVerticle.class);
  }
}
