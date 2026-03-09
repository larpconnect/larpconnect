package com.larpconnect.njall.common;

import com.google.common.base.Stopwatch;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.common.codec.ProtoCodec;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.common.time.MonotonicClock;
import com.larpconnect.njall.common.time.TimeService;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

final class CommonBindingModule extends AbstractModule {
  CommonBindingModule() {}

  @Override
  @SuppressWarnings("unchecked")
  protected void configure() {
    bind(JsonFormat.Printer.class)
        .toInstance(
            JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace());
    bind(JsonFormat.Parser.class).toInstance(JsonFormat.parser().ignoringUnknownFields());

    // TimeModule bindings
    try {
      bind(MonotonicClock.class).to(TimeService.class);
      bind(TimeService.class)
          .to(
              (Class<? extends TimeService>)
                  Class.forName("com.larpconnect.njall.common.time.MonotonicTimeService"))
          .in(Singleton.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Could not find MonotonicTimeService", e);
    }

    // IdModule bindings
    try {
      bind(IdGenerator.class)
          .to(
              (Class<? extends IdGenerator>)
                  Class.forName("com.larpconnect.njall.common.id.UuidV7Generator"));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Could not find UuidV7Generator", e);
    }

    // CodecModule bindings
    try {
      bind(ProtoCodec.class)
          .to(
              (Class<? extends ProtoCodec>)
                  Class.forName("com.larpconnect.njall.common.codec.ProtoCodecRegistry"));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Could not find ProtoCodecRegistry", e);
    }
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
