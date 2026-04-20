package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;

/** Guice module responsible for providing unique identifier generation capabilities. */
public final class IdModule extends AbstractModule {
  /**
   * Constructs a new {@link IdModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public IdModule() {}

  @Override
  protected void configure() {
    install(new IdBindingModule());
  }
}
