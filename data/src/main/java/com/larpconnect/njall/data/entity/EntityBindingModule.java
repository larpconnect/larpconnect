package com.larpconnect.njall.data.entity;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(EntityModule.class)
final class EntityBindingModule extends AbstractModule {
  EntityBindingModule() {}

  @Override
  protected void configure() {
    // Entities typically don't need direct bindings as they are created dynamically or fetched via
    // DAOs
  }
}
