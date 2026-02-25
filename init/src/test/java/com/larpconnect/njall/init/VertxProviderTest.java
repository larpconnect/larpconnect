package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
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
  void close_successful() {
    when(mockVertx.close()).thenReturn(Future.succeededFuture());

    provider.get(); // Initialize
    var future = provider.close();

    verify(mockVertx).close();
    assertThat(future.succeeded()).isTrue();
  }

  @Test
  void close_failure_logsError() {
    var cause = new RuntimeException("fail");
    when(mockVertx.close()).thenReturn(Future.failedFuture(cause));

    provider.get();
    var future = provider.close();

    verify(mockVertx).close();
    assertThat(future.failed()).isTrue();
    assertThat(future.cause()).isSameAs(cause);
  }

  @Test
  void close_notInitialized_doesNothing() {
    // Ensure no NPE or interaction if get() was never called
    var future = provider.close();
    assertThat(future.succeeded()).isTrue();
  }
}
