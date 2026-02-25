package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class VerticleLifecycleTest {

  @Test
  public void startUp_validConfig_success() throws Exception {
    var lifecycle = VerticleServices.create(Collections.emptyList());

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isTrue();

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  public void shutDown_closesVertx() {
    var mockVertx = mock(Vertx.class);
    var mockEventBus = mock(EventBus.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);
    when(mockVertx.close()).thenReturn(Future.succeededFuture());

    var mockProvider = mock(VertxProvider.class);
    when(mockProvider.get()).thenReturn(mockVertx);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);
    lifecycle.startAsync().awaitRunning();
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx).close();
  }

  @Test
  public void shutDown_getReturnsNull_doesNothing() {
    var mockProvider = mock(VertxProvider.class);
    when(mockProvider.get()).thenReturn(null);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);

    var mockVertx = mock(Vertx.class);
    var mockEventBus = mock(EventBus.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);

    when(mockProvider.get())
        .thenReturn(mockVertx) // First call in startUp
        .thenReturn(null); // Second call in shutDown

    lifecycle.startAsync().awaitRunning();
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx, never()).close();
  }

  @Test
  public void shutDown_closeFails_logsError() {
    var mockVertx = mock(Vertx.class);
    var mockEventBus = mock(EventBus.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);
    when(mockVertx.close()).thenReturn(Future.failedFuture("failure"));

    var mockProvider = mock(VertxProvider.class);
    when(mockProvider.get()).thenReturn(mockVertx);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);
    lifecycle.startAsync().awaitRunning();
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx).close();
  }

  @Test
  public void deploy_delegatesToService() {
    var mockVertx = mock(Vertx.class);
    var mockEventBus = mock(EventBus.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);
    // Mock deployment success
    when(mockVertx.deployVerticle(anyString())).thenReturn(Future.succeededFuture("id"));
    when(mockVertx.close()).thenReturn(Future.succeededFuture());

    var mockProvider = mock(VertxProvider.class);
    when(mockProvider.get()).thenReturn(mockVertx);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);
    lifecycle.startAsync().awaitRunning();
    lifecycle.deploy(TestVerticle.class);
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx).deployVerticle("guice:" + TestVerticle.class.getName());
  }

  @Test
  public void deploy_notStarted_throwsException() {
    var lifecycle = new VerticleLifecycle(Collections.emptyList());
    assertThatThrownBy(() -> lifecycle.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("VerticleLifecycle not started");
  }

  static final class TestVerticle extends AbstractVerticle {}
}
