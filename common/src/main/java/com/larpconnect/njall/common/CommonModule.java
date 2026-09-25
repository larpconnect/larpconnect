package com.larpconnect.njall.common;

import static java.util.Objects.requireNonNull;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.config.ConfigModule;
import com.larpconnect.njall.common.health.HealthModule;
import com.larpconnect.njall.common.telemetry.TelemetryModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/** Top-level Guice module for the common subproject, installing subpackage modules. */
public final class CommonModule extends AbstractModule {

  private final Config config;

  public CommonModule() {
    this(ConfigFactory.load());
  }

  public CommonModule(Config config) {
    this.config = requireNonNull(config, "config cannot be null");
  }

  @Override
  protected void configure() {
    install(new ConfigModule(config));
    install(new HealthModule());
    install(new TelemetryModule());
  }
}
