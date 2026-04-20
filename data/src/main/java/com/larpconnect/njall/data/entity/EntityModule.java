package com.larpconnect.njall.data.entity;

import com.google.inject.AbstractModule;

/** Guice module for configuring database entities. */
public final class EntityModule extends AbstractModule {
  /**
   * Constructs a new {@link EntityModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public EntityModule() {}

  @Override
  protected void configure() {
    install(new EntityBindingModule());
  }
}
