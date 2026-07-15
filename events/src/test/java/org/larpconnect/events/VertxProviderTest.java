package org.larpconnect.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.Vertx;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Unit tests for VertxProvider verifying thread safety and singleton behavior. */
public final class VertxProviderTest {
  @Test
  public void get_returnsNonNullInstance() {
    GuiceVerticleFactory factory = mock(GuiceVerticleFactory.class);
    when(factory.prefix()).thenReturn(GuiceVerticleFactory.PREFIX);

    VertxProvider provider = new VertxProvider(() -> factory);
    Vertx vertx = provider.get();
    try {
      assertThat(vertx).isNotNull();
    } finally {
      vertx.close();
    }
  }

  @Test
  public void get_subsequentCalls_returnSameInstance() {
    GuiceVerticleFactory factory = mock(GuiceVerticleFactory.class);
    when(factory.prefix()).thenReturn(GuiceVerticleFactory.PREFIX);

    VertxProvider provider = new VertxProvider(() -> factory);
    Vertx vertx1 = provider.get();
    Vertx vertx2 = provider.get();
    try {
      assertThat(vertx1).isSameAs(vertx2);
    } finally {
      vertx1.close();
    }
  }

  @Test
  public void get_isThreadSafe() throws InterruptedException {
    GuiceVerticleFactory factory = mock(GuiceVerticleFactory.class);
    when(factory.prefix()).thenReturn(GuiceVerticleFactory.PREFIX);

    VertxProvider provider = new VertxProvider(() -> factory);
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(1);
    java.util.List<AtomicReference<Vertx>> results = new java.util.ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      AtomicReference<Vertx> ref = new AtomicReference<>();
      results.add(ref);
      executor.execute(
          () -> {
            try {
              latch.await();
              ref.set(provider.get());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
    }

    latch.countDown();
    executor.shutdown();
    boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(finished).isTrue();

    Vertx expected = results.get(0).get();
    assertThat(expected).isNotNull();
    for (int i = 1; i < threadCount; i++) {
      assertThat(results.get(i).get()).isSameAs(expected);
    }

    if (expected != null) {
      expected.close();
    }
  }
}
