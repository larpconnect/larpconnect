package com.larpconnect.njall.common.codec;

import com.google.inject.AbstractModule;

/** Guice module responsible for registering Protocol Buffer codecs for the Vert.x EventBus. */
public final class CodecModule extends AbstractModule {
  /**
   * Constructs a new {@link CodecModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation by upper-level
   * modules while keeping the internal binding modules encapsulated within this package.
   */
  public CodecModule() {}

  @Override
  protected void configure() {
    install(new CodecBindingModule());
  }
}
