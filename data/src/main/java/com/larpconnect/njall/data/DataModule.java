package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;

/** Main module for the data layer. Installs bindings for Hibernate Reactive. */
public final class DataModule extends AbstractModule {

  /** Constructor. */
  public DataModule() {}

  @Override
  protected void configure() {
    install(new DataBindingModule());
  }
}
