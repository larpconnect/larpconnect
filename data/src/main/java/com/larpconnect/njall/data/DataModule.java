package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.data.dao.DaoModule;
import com.larpconnect.njall.data.entity.EntityModule;

/** Guice module for configuring data access layer dependencies. */
public final class DataModule extends AbstractModule {
  /**
   * Constructs a new {@link DataModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public DataModule() {}

  @Override
  protected void configure() {
    install(new DataBindingModule());
    install(new EntityModule());
    install(new DaoModule());
  }
}
