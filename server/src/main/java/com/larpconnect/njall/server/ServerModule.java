package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.function.Consumer;

public final class ServerModule extends AbstractModule {
  private static final int DEFAULT_PORT = 8080;

  public ServerModule() {}

  @Override
  protected void configure() {
    bind(new TypeLiteral<Optional<Consumer<Integer>>>() {}).toInstance(Optional.empty());

    bind(MainVerticle.class).to(DefaultMainVerticle.class).in(Scopes.SINGLETON);
    var verticles = Multibinder.newSetBinder(binder(), Verticle.class);
    verticles.addBinding().to(WebServerVerticle.class);
  }

  @Provides
  @Singleton
  @Named("web.port")
  int provideWebPort(JsonObject config) {
    return config.getInteger("web.port", DEFAULT_PORT);
  }

  @Provides
  @Singleton
  @Named("openapi.spec")
  String provideOpenApiSpec(JsonObject config) {
    return config.getString("openapi.spec", "openapi.yaml");
  }
}
