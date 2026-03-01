package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;

public final class IdModule extends AbstractModule {
  public IdModule() {}

  @Override
  protected void configure() {
    install(new IdBindingModule());
  }
}
