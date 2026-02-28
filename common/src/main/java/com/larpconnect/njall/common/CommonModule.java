package com.larpconnect.njall.common;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.time.TimeModule;

public final class CommonModule extends AbstractModule {
  public CommonModule() {}

  @Override
  protected void configure() {
    install(new TimeModule());
  }
}
