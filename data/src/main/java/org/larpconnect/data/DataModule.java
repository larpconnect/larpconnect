package org.larpconnect.data;

import com.google.inject.AbstractModule;

/** Exposes bindings for the database layer (Hibernate/PostgreSQL). */
public final class DataModule extends AbstractModule {
  @Override
  protected void configure() {
    // Bindings for repositories, entity managers, and session factories
  }
}
