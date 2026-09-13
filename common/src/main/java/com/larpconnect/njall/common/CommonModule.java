package com.larpconnect.njall.common;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.config.ConfigModule;

/** Top-level Guice module for the common subproject, installing subpackage modules. */
public final class CommonModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new ConfigModule());
  }
}
