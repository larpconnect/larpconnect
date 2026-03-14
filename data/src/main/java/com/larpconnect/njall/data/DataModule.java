package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;

/**
 * Guice module for configuring database access components.
 *
 * <p>This module sets up Hibernate Reactive with Vert.x PostgreSQL client.
 */
public final class DataModule extends AbstractModule {

  public DataModule() {}

  @Override
  protected void configure() {
    install(new DataBindingModule());
  }
}
