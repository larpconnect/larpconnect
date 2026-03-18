package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.common.annotations.InstallInstead;
import com.larpconnect.njall.proto.LarpconnectConfig;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@InstallInstead(ServerModule.class)
final class ServerBindingModule extends AbstractModule {
  private final Function<String, String> getenv;
  private static final int DEFAULT_PORT = 8080;
  private static final String DEFAULT_SPEC = "openapi.yaml";

  ServerBindingModule() {
    this(System::getenv);
  }

  ServerBindingModule(Function<String, String> getenv) {
    this.getenv = getenv;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<Optional<Consumer<Integer>>>() {}).toInstance(Optional.empty());

    bind(MainVerticle.class).to(DefaultMainVerticle.class);
    var verticles = Multibinder.newSetBinder(binder(), Verticle.class);
    verticles.addBinding().to(WebServerVerticle.class);
  }

  @Provides
  @Singleton
  LarpconnectConfig provideLarpconnectConfig(JsonObject config) {
    var builder = LarpconnectConfig.newBuilder();

    var appConfig = config.getJsonObject("larpconnect");
    int port = DEFAULT_PORT;
    String spec = DEFAULT_SPEC;

    if (appConfig != null) {
      port = appConfig.getInteger("web.port", DEFAULT_PORT);
      spec = appConfig.getString("openapi.spec", DEFAULT_SPEC);
    } else {
      port = config.getInteger("web.port", DEFAULT_PORT);
      spec = config.getString("openapi.spec", DEFAULT_SPEC);
    }

    var envPort = getenv.apply("PORT");
    if (envPort != null) {
      try {
        port = Integer.parseInt(envPort);
      } catch (NumberFormatException ignored) {
        // Fall back to config if env var is not a valid integer
      }
    }

    builder.setWebPort(port);
    builder.setOpenapiSpec(spec);

    return builder.build();
  }
}
