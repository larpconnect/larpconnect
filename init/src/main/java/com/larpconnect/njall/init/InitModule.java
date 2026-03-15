package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;

/**
 * Guice module responsible for providing application initialization, configuration, and Verticle
 * deployment services.
 */
public final class InitModule extends AbstractModule {
  /**
   * Constructs a new {@link InitModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings
   * (e.g. {@link InitBindingModule}).
   */
  public InitModule() {}

  @Override
  protected void configure() {
    install(new InitBindingModule());
  }
}
