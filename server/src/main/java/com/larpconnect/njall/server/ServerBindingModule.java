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
import java.util.function.Function;

final class ServerBindingModule extends AbstractModule {
  private final Function<String, String> getenv;
  private static final int DEFAULT_PORT = 8080;

  /** Constructs a new ServerBindingModule. */
  public ServerBindingModule() {
    this(System::getenv);
  }

  ServerBindingModule(Function<String, String> getenv) {
    this.getenv = getenv;
  }

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
    String envPort = getenv.apply("PORT");
    if (envPort != null) {
      try {
        return Integer.parseInt(envPort);
      } catch (NumberFormatException ignored) {
        // Fall back to config if env var is not a valid integer
      }
    }
    var appConfig = config.getJsonObject("larpconnect");
    if (appConfig != null) {
      return appConfig.getInteger("web.port", DEFAULT_PORT);
    }
    return config.getInteger("web.port", DEFAULT_PORT);
  }

  @Provides
  @Singleton
  @Named("openapi.spec")
  String provideOpenApiSpec(JsonObject config) {
    var appConfig = config.getJsonObject("larpconnect");
    if (appConfig != null) {
      return appConfig.getString("openapi.spec", "openapi.yaml");
    }
    return config.getString("openapi.spec", "openapi.yaml");
  }
}
