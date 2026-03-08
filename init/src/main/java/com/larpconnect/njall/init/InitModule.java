package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;

public final class InitModule extends AbstractModule {
  public InitModule() {}

  @Override
  protected void configure() {
    install(new InitBindingModule());
  }
}
