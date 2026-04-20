package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;

/** Guice module for configuring Data Access Objects (DAOs). */
public final class DaoModule extends AbstractModule {
  /**
   * Constructs a new {@link DaoModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public DaoModule() {}

  @Override
  protected void configure() {
    install(new DaoBindingModule());
  }
}
