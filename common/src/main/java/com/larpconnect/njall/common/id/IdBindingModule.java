package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(IdModule.class)
final class IdBindingModule extends AbstractModule {
  IdBindingModule() {}

  @Override
  protected void configure() {
    bind(IdGenerator.class).to(UuidV7Generator.class);
  }
}
