package com.larpconnect.njall.common;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.time.TimeModule;

/** Guice module for common utilities. */
public final class CommonModule extends AbstractModule {

  /** Constructs a new CommonModule. */
  public CommonModule() {}

  @Override
  protected void configure() {
    install(new TimeModule());
  }
}
