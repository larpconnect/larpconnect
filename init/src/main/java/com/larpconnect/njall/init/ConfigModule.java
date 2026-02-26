package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import io.vertx.core.json.JsonObject;

final class ConfigModule extends AbstractModule {
  private final JsonObject config;

  ConfigModule(JsonObject config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(JsonObject.class).toInstance(config);
  }
}
