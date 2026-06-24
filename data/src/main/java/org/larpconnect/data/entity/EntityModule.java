package org.larpconnect.data.entity;

import com.google.inject.AbstractModule;

/** Guice module for entity packages. */
public final class EntityModule extends AbstractModule {
  @Override
  protected void configure() {
    // Entities do not require direct Guice bindings, but this module satisfies topological rules.
  }
}
