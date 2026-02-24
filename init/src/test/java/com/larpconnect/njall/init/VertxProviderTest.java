package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;
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
    Vertx result = provider.get();
    assertThat(result).isSameAs(mockVertx);
  }

  @Test
  void get_returnsSameInstance() {
    AtomicInteger count = new AtomicInteger(0);
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
  void close_successful() {
    when(mockVertx.close()).thenReturn(Future.succeededFuture());

    provider.get(); // Initialize
    provider.close();

    verify(mockVertx).close();
  }

  @Test
  void close_failure_logsError() {
    RuntimeException cause = new RuntimeException("fail");
    when(mockVertx.close()).thenReturn(Future.failedFuture(cause));

    provider.get();
    provider.close();

    verify(mockVertx).close();
  }

  @Test
  void close_timeout() {
    // Mock close() to never complete
    when(mockVertx.close()).thenReturn(Future.future(p -> {}));

    // Use short timeout
    provider = new VertxProvider(mockFactory, 10, TimeUnit.MILLISECONDS);
    provider.get();

    long start = System.currentTimeMillis();
    provider.close();
    long duration = System.currentTimeMillis() - start;

    // It should wait at least 10ms
    assertThat(duration).isGreaterThanOrEqualTo(10);
  }

  @Test
  void close_interrupted() {
    // Mock close() to never complete so it waits
    when(mockVertx.close()).thenReturn(Future.future(p -> {}));

    provider = new VertxProvider(mockFactory, 1, TimeUnit.SECONDS);
    provider.get();

    Thread.currentThread().interrupt();
    provider.close();

    // Verify interrupt status is restored
    assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void close_notInitialized_doesNothing() {
    // Ensure no NPE or interaction if get() was never called
    provider.close();
    // No strict verification needed other than no exception
  }
}
