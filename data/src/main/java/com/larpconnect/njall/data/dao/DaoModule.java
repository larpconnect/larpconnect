package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;

/** Guice module for configuring Data Access Objects (DAOs). */
public final class DaoModule extends AbstractModule {
  public DaoModule() {}

  @Override
  protected void configure() {
    install(new DaoBindingModule());
  }
}
