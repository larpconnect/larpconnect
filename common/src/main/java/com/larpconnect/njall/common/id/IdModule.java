package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;

/** Guice module responsible for providing unique identifier generation capabilities. */
public final class IdModule extends AbstractModule {
  /**
   * Constructs a new {@link IdModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation by upper-level
   * modules while keeping the internal binding modules encapsulated within this package.
   */
  public IdModule() {}

  @Override
  protected void configure() {
    install(new IdBindingModule());
  }
}
