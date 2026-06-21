package org.larpconnect.test;

import com.google.inject.AbstractModule;

/** Exposes bindings for shared test infrastructure components. */
public final class TestModule extends AbstractModule {
  @Override
  protected void configure() {
    // Bindings for mock databases or mock external services
  }
}
