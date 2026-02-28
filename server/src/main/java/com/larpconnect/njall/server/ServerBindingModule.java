package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.Verticle;
import java.util.Optional;
import java.util.function.Consumer;

final class ServerBindingModule extends AbstractModule {

  /** Constructs a new ServerBindingModule. */
  public ServerBindingModule() {}

  @Override
  protected void configure() {
    bind(new TypeLiteral<Optional<Consumer<Integer>>>() {}).toInstance(Optional.empty());

    bind(MainVerticle.class).to(DefaultMainVerticle.class).in(Scopes.SINGLETON);
    var verticles = Multibinder.newSetBinder(binder(), Verticle.class);
    verticles.addBinding().to(WebServerVerticle.class);
  }
}
