package com.larpconnect.njall.common;

import com.google.common.base.Stopwatch;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.protobuf.util.JsonFormat;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

final class CommonBindingModule extends AbstractModule {
  CommonBindingModule() {}

  @Override
  protected void configure() {
    bind(JsonFormat.Printer.class)
        .toInstance(
            JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace());
    bind(JsonFormat.Parser.class).toInstance(JsonFormat.parser().ignoringUnknownFields());
  }

  @Provides
  @Singleton
  Clock provideClock() {
    return Clock.systemUTC();
  }

  @Provides
  Stopwatch provideStopwatch() {
    return Stopwatch.createUnstarted();
  }

  @Provides
  RandomGenerator provideRandomGenerator() {
    return ThreadLocalRandom.current();
  }
}
