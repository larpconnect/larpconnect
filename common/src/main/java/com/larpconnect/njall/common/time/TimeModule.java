package com.larpconnect.njall.common.time;

import com.google.inject.AbstractModule;

/** Guice module responsible for providing time-related services and monotonic clocks. */
public final class TimeModule extends AbstractModule {
  /**
   * Constructs a new {@link TimeModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation by upper-level
   * modules while keeping the internal binding modules encapsulated within this package.
   */
  public TimeModule() {}

  @Override
  protected void configure() {
    install(new TimeBindingModule());
  }
}
