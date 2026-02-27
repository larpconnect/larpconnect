package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.json.JsonObject;

final class ConfigModule extends AbstractModule {
  private final JsonObject config;

  ConfigModule(JsonObject config) {
    this.config = config;
  }

  @Override
  @AiContract(
      ensure = "JsonObject \\text{ is bound to } config",
      implementationHint = "Binds the configuration JsonObject to the instance.")
  protected void configure() {
    bind(JsonObject.class).toInstance(config);
  }
}
