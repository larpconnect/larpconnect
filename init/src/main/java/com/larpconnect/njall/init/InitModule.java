package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;

/**
 * Guice module responsible for providing application initialization, configuration, and Verticle
 * deployment services.
 */
public final class InitModule extends AbstractModule {
  public InitModule() {}

  @Override
  protected void configure() {
    install(new InitBindingModule());
  }
}
