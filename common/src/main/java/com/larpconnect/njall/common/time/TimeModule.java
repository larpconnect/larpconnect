package com.larpconnect.njall.common.time;

import com.google.inject.AbstractModule;

/** Guice module responsible for providing time-related services and monotonic clocks. */
public final class TimeModule extends AbstractModule {
  public TimeModule() {}

  @Override
  protected void configure() {
    install(new TimeBindingModule());
  }
}
