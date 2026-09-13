package com.larpconnect.njall.common;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.config.ConfigModule;
import com.larpconnect.njall.common.health.HealthModule;

/** Top-level Guice module for the common subproject, installing subpackage modules. */
public final class CommonModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new ConfigModule());
    install(new HealthModule());
  }
}
