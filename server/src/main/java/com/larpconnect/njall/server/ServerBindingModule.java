package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.common.annotations.InstallInstead;
import com.larpconnect.njall.proto.LarpConnectConfig;
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
  LarpConnectConfig provideLarpConnectConfig(JsonObject config) {
    var builder = LarpConnectConfig.newBuilder();

    JsonObject sourceConfig = config;
    if (config.containsKey("larpconnect")) {
      sourceConfig = config.getJsonObject("larpconnect");
    }

    try {
      JsonFormat.parser().ignoringUnknownFields().merge(sourceConfig.encode(), builder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Failed to parse LarpConnectConfig", e);
    }

    if (builder.getWebPort() == 0) {
      builder.setWebPort(DEFAULT_PORT);
    }
    if (builder.getOpenapiSpec().isEmpty()) {
      builder.setOpenapiSpec(DEFAULT_SPEC);
    }

    var envPort = getenv.apply("PORT");
    if (envPort != null) {
      try {
        builder.setWebPort(Integer.parseInt(envPort));
      } catch (NumberFormatException ignored) {
        // Fall back to config if env var is not a valid integer
      }
    }

    return builder.build();
  }
}
