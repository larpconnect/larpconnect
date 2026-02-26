package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.vertx.core.Verticle;
import java.util.Optional;
import java.util.function.Consumer;

public final class ServerModule extends AbstractModule {
  private static final int DEFAULT_PORT = 8080;

  public ServerModule() {}

  @Override
  protected void configure() {
    bindConstant().annotatedWith(Names.named("web.port")).to(DEFAULT_PORT);
    bindConstant().annotatedWith(Names.named("openapi.spec")).to("openapi.yaml");
    bind(new TypeLiteral<Optional<Consumer<Integer>>>() {}).toInstance(Optional.empty());

    bind(MainVerticle.class).to(DefaultMainVerticle.class).in(Scopes.SINGLETON);
    var verticles = Multibinder.newSetBinder(binder(), Verticle.class);
    verticles.addBinding().to(WebServerVerticle.class);
  }
}
