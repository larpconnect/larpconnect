package com.larpconnect.njall.common.time;

import com.google.inject.AbstractModule;

public final class TimeModule extends AbstractModule {
  public TimeModule() {}

  @Override
  protected void configure() {
    install(new TimeBindingModule());
  }
}
