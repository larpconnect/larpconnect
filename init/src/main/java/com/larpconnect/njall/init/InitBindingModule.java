package com.larpconnect.njall.init;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(InitModule.class)
final class InitBindingModule extends AbstractModule {
  InitBindingModule() {}

  @Override
  protected void configure() {}
}
