package com.larpconnect.njall.api;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.api.verticle.ApiVerticleModule;

/**
 * Guice module for the API module. Exposing it allows other modules to install it and bypasses the
 * ArchUnit public class restriction which permits Modules to be public.
 */
public final class ApiModule extends AbstractModule {
  /**
   * Constructs a new {@link ApiModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public ApiModule() {}

  @Override
  protected void configure() {
    install(new ApiBindingModule());
    install(new ApiVerticleModule());
  }
}
