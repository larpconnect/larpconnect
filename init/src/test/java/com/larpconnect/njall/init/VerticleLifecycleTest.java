package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

final class VerticleLifecycleTest {
  @Test
  public void deploy_notStarted_throwsException() {
    var mockVertx = mock(Vertx.class);
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector);

    assertThatThrownBy(() -> lifecycle.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("VerticleLifecycle not started");
  }

  @Test
  public void shutDown_closeNotStarted_doesNothing() {
    var mockVertx = mock(Vertx.class);
    when(mockVertx.close()).thenReturn(io.vertx.core.Future.succeededFuture());
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector);

    assertThatCode(lifecycle::shutDown).doesNotThrowAnyException();
  }

  @Test
  public void shutDown_closeFails_doesNotThrowException() {
    var mockVertx = mock(Vertx.class);
    when(mockVertx.close())
        .thenReturn(io.vertx.core.Future.failedFuture(new RuntimeException("Close failed")));
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector);

    assertThatCode(lifecycle::shutDown).doesNotThrowAnyException();
  }

  @Test
  public void shutDown_interrupted_doesNotThrowException() {
    var mockVertx = mock(Vertx.class);
    // Never complete the future so latch.await() blocks
    var promise = io.vertx.core.Promise.<Void>promise();
    when(mockVertx.close()).thenReturn(promise.future());
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector);

    Thread.currentThread().interrupt();
    try {
      assertThatCode(lifecycle::shutDown).doesNotThrowAnyException();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      // Clear interrupt status for other tests
      Thread.interrupted();
    }
  }

  @Test
  public void shutDown_interruptedWhileWaiting_restoresInterruptStatus() throws Exception {
    var mockVertx = mock(Vertx.class);
    var promise = io.vertx.core.Promise.<Void>promise();
    when(mockVertx.close()).thenReturn(promise.future());
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector);

    var startedLatch = new java.util.concurrent.CountDownLatch(1);
    var interruptStatus = new java.util.concurrent.atomic.AtomicBoolean(false);

    var thread =
        new Thread(
            () -> {
              startedLatch.countDown();
              lifecycle.shutDown();
              interruptStatus.set(Thread.currentThread().isInterrupted());
              // Clear the interrupt status so it doesn't leak
              Thread.interrupted();
            });
    thread.start();

    // Use a small loop or awaitility if possible, but standard latch wait is ok
    startedLatch.await();

    // We cannot easily determine when shutDown() has entered latch.await(),
    // so we wait a short moment and then interrupt, or repeatedly interrupt until
    // the thread completes.
    // An alternative that doesn't use sleep is to repeatedly call interrupt
    // and wait for the thread to join with a timeout.
    // If it joins, we are done.
    while (thread.isAlive()) {
      thread.interrupt();
      thread.join(10);
    }

    assertThat(interruptStatus.get()).isTrue();
  }

  static final class TestVerticle extends AbstractVerticle {}
}
