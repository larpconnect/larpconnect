package com.larpconnect.njall.common;

import com.google.inject.AbstractModule;

public final class CommonModule extends AbstractModule {
  public CommonModule() {}

  @Override
  protected void configure() {
    install(new CommonBindingModule());
  }
}
