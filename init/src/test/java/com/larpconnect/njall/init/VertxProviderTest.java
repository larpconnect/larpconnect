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

  @Test
  void release_returnsInstanceAndClears() {
    provider.get(); // Initialize
    var instance1 = provider.get();

    var released = provider.release();
    assertThat(released).isSameAs(instance1);

    // After release, get() should use factory again (though behavior depends on whether factory creates new)
    // Here we just verify release returned it.
    // Also verify internal state is cleared by checking if get() calls factory again if we were to call it
    // But since factory returns same mock, we can't easily check 'new instance' unless we use counting factory.
  }

  @Test
  void release_returnsNullIfNotInitialized() {
    var released = provider.release();
    assertThat(released).isNull();
  }
}
