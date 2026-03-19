package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.data.dao.DaoModule;
import com.larpconnect.njall.data.entity.EntityModule;

/** Guice module for configuring data access layer dependencies. */
public final class DataModule extends AbstractModule {
  public DataModule() {}

  @Override
  protected void configure() {
    install(new DataBindingModule());
    install(new EntityModule());
    install(new DaoModule());
  }
}
