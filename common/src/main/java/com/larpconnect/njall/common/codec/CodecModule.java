package com.larpconnect.njall.common.codec;

import com.google.inject.AbstractModule;

/** Guice module responsible for registering Protocol Buffer codecs for the Vert.x EventBus. */
public final class CodecModule extends AbstractModule {
  public CodecModule() {}

  @Override
  protected void configure() {
    install(new CodecBindingModule());
  }
}
