package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;

/** Guice module for configuring Data Access Objects (DAOs). */
public final class DaoModule extends AbstractModule {
  /**
   * Constructs a new {@link DaoModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation while
   * encapsulating internal package-private bindings.
   */
  public DaoModule() {}

  @Override
  protected void configure() {
    install(new DaoBindingModule());
  }
}
