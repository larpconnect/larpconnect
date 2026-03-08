package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(InitModule.class)
final class InitBindingModule extends AbstractModule {
  InitBindingModule() {}

  @Override
  protected void configure() {
    // VertxModule and ConfigModule require state initialized dynamically during
    // VerticleLifecycle startup, so they are injected directly by the lifecycle
    // instead of statically bound here.
  }
}
