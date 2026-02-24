package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class VerticleLifecycleTest {

  @Test
  public void startUp_validConfig_success() throws Exception {
    VerticleService lifecycle = VerticleServices.create(Collections.emptyList());

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isTrue();

    Vertx vertx = lifecycle.getVertx();
    assertThat(vertx).isNotNull();
    assertThat(lifecycle.getInjector()).isNotNull();

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  public void getInjector_providesVertxInstance() throws TimeoutException {
    VerticleService lifecycle = VerticleServices.create(Collections.emptyList());
    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.getInjector().getInstance(Vertx.class)).isNotNull();
    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
  }

  @Test
  public void shutDown_failure_logsError() throws TimeoutException {
    Vertx mockVertx = mock(Vertx.class);
    EventBus mockEventBus = mock(EventBus.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);
    when(mockVertx.close()).thenReturn(Future.failedFuture("Simulated failure"));

    VerticleLifecycle lifecycle =
        new VerticleLifecycle(Collections.emptyList(), (Supplier<Vertx>) () -> mockVertx);
    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    verify(mockVertx).close();
  }

  @Test
  public void shutDown_whenNotStarted_noOps() throws Exception {
    VerticleLifecycle lifecycle = new VerticleLifecycle(Collections.emptyList());
    java.lang.reflect.Method method = VerticleLifecycle.class.getDeclaredMethod("shutDown");
    method.setAccessible(true);
    method.invoke(lifecycle);
  }

  @Test
  public void shutDown_timeout_logsWarning() throws TimeoutException {
    Vertx mockVertx = mock(Vertx.class);
    EventBus mockEventBus = mock(EventBus.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);

    // Return a future that never completes
    when(mockVertx.close()).thenReturn(Future.future(promise -> {}));

    VerticleLifecycle lifecycle =
        new VerticleLifecycle(Collections.emptyList(), (Supplier<Vertx>) () -> mockVertx);
    // Set a very short timeout for testing
    lifecycle.setShutdownTimeoutSeconds(0);

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    verify(mockVertx).close();
  }
}
