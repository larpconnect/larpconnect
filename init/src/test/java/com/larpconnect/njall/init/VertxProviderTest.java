package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vertx.core.Vertx;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VertxProviderTest {

  private Vertx mockVertx;
  private Supplier<Vertx> mockFactory;
  private VertxProvider provider;

  @BeforeEach
  void setUp() {
    mockVertx = mock(Vertx.class);
    mockFactory = () -> mockVertx;
    provider = new VertxProvider(mockFactory);
  }

  @Test
  void get_createsNewInstance() {
    var result = provider.get();
    assertThat(result).isSameAs(mockVertx);
  }

  @Test
  void get_returnsSameInstance() {
    var count = new AtomicInteger(0);
    Supplier<Vertx> countingFactory =
        () -> {
          count.incrementAndGet();
          return mockVertx;
        };
    provider = new VertxProvider(countingFactory);

    provider.get();
    provider.get();

    assertThat(count.get()).isEqualTo(1);
  }
}
