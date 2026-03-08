package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.larpconnect.njall.common.annotations.InstallInstead;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@InstallInstead(IdModule.class)
final class IdBindingModule extends AbstractModule {
  IdBindingModule() {}

  @Override
  protected void configure() {
    bind(IdGenerator.class).to(IdGeneratorService.class);
    bind(IdGeneratorService.class).to(UuidV7GeneratorService.class);
  }

  @Provides
  RandomGenerator provideRandomGenerator() {
    return ThreadLocalRandom.current();
  }
}
