package org.larpconnect.base;

import com.google.inject.AbstractModule;

/** Exposes bindings for the database-to-Vert.x worker bridge. */
public final class BaseModule extends AbstractModule {
  @Override
  protected void configure() {
    // Bindings mapping blocking database calls to Vert.x worker executors
  }
}
