package com.larpconnect.njall.common.id;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

final class IdBindingModule extends AbstractModule {
  IdBindingModule() {}

  @Override
  protected void configure() {
    bind(IdGenerator.class).to(UuidV7Generator.class);
  }

  @Provides
  RandomGenerator provideRandomGenerator() {
    return ThreadLocalRandom.current();
  }
}
