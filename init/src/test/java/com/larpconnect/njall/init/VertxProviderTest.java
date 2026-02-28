package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class VertxProviderTest {
  private Vertx mockVertx;

  @BeforeEach
  void setUp() {
    mockVertx = mock(Vertx.class);
  }

  @Test
  void get_returnsSingletonVertx() {
    Supplier<Vertx> factory = () -> mockVertx;
    var provider = new VertxProvider(factory);

    var first = provider.get();
    var second = provider.get();

    assertThat(first).isSameAs(mockVertx);
    assertThat(second).isSameAs(first);
  }

  @Test
  void get_multipleThreads_returnsSameInstance() throws InterruptedException {
    AtomicInteger calls = new AtomicInteger(0);
    Supplier<Vertx> factory =
        () -> {
          calls.incrementAndGet();
          return mockVertx;
        };
    var provider = new VertxProvider(factory);

    Thread t1 = new Thread(provider::get);
    Thread t2 = new Thread(provider::get);
    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertThat(calls.get()).isEqualTo(1);
    assertThat(provider.get()).isSameAs(mockVertx);
  }

  @Test
  void constructWithJsonObject_works() {
    // Cannot easily test Vertx.vertx(options) without starting a real instance,
    // so we just instantiate the class and ensure no exceptions.
    var provider = new VertxProvider(new JsonObject());
    assertThat(provider).isNotNull();
  }
}
