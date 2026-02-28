package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;

final class ServerProvidesModule extends AbstractModule {
  private static final int DEFAULT_PORT = 8080;

  /** Constructs a new ServerProvidesModule. */
  public ServerProvidesModule() {}

  @Provides
  @Singleton
  @Named("web.port")
  int provideWebPort(JsonObject config) {
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
