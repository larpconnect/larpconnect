package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.data.dao.DaoModule;
import com.larpconnect.njall.data.entity.EntityModule;

/** Guice module for configuring data access layer dependencies. */
public final class DataModule extends AbstractModule {
  /**
   * Constructs a new {@link DataModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation by upper-level
   * modules while keeping the internal binding modules encapsulated within this package.
   */
  public DataModule() {}

  @Override
  protected void configure() {
    install(new DataBindingModule());
    install(new EntityModule());
    install(new DaoModule());
  }
}
