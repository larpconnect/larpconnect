package com.larpconnect.njall.common.time;

import com.google.inject.AbstractModule;

/** Guice module responsible for providing time-related services and monotonic clocks. */
public final class TimeModule extends AbstractModule {
  /**
   * Constructs a new {@link TimeModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public TimeModule() {}

  @Override
  protected void configure() {
    install(new TimeBindingModule());
  }
}
