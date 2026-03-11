package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;

/** Guice module responsible for providing unique identifier generation capabilities. */
public final class IdModule extends AbstractModule {
  public IdModule() {}

  @Override
  protected void configure() {
    install(new IdBindingModule());
  }
}
