package com.larpconnect.njall.data.entity;

import com.google.inject.AbstractModule;

/** Guice module for configuring database entities. */
public final class EntityModule extends AbstractModule {
  /**
   * Constructs a new {@link EntityModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation by upper-level
   * modules while keeping the internal binding modules encapsulated within this package.
   */
  public EntityModule() {}

  @Override
  protected void configure() {
    install(new EntityBindingModule());
  }
}
