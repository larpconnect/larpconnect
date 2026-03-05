package com.larpconnect.njall.api;

import com.google.inject.AbstractModule;

/**
 * Guice module for the API module. Exposing it allows other modules to install it and bypasses the
 * ArchUnit public class restriction which permits Modules to be public.
 */
public final class ApiModule extends AbstractModule {
  public ApiModule() {}

  @Override
  protected void configure() {
    // Guice bindings can be added here
  }
}
