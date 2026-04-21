package com.larpconnect.njall.common.codec;

import com.google.inject.AbstractModule;

/** Guice module responsible for registering Protocol Buffer codecs for the Vert.x EventBus. */
public final class CodecModule extends AbstractModule {
  /**
   * Constructs a new {@link CodecModule}.
   *
   * <p>This constructor is intentionally public to allow cross-package installation while
   * encapsulating internal package-private bindings.
   */
  public CodecModule() {}

  @Override
  protected void configure() {
    install(new CodecBindingModule());
  }
}
