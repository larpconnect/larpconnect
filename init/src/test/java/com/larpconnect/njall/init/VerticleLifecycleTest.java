package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class VerticleLifecycleTest {

  @Test
  public void startUp_validConfig_success() throws Exception {
    var lifecycle =
        VerticleServices.create(
            ImmutableList.of(
                new com.larpconnect.njall.common.CommonModule(),
                new com.larpconnect.njall.common.codec.CodecModule()));

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isTrue();

    lifecycle.deploy(TestVerticle.class);

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  public void startUp_missingConfig_throwsRuntimeException() {
    System.setProperty("njall.config.resource", "missing.json");
    try {
      var lifecycle =
          VerticleServices.create(
              ImmutableList.of(
                  new com.larpconnect.njall.common.CommonModule(),
                  new com.larpconnect.njall.common.codec.CodecModule()));
      assertThatThrownBy(() -> lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS))
          .isInstanceOf(IllegalStateException.class);
    } finally {
      System.clearProperty("njall.config.resource");
    }
  }

  @Test
  public void deploy_notStarted_throwsException() {
    var mockVertx = mock(Vertx.class);
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(mockVertx, mockSetupService, mockInjector);

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
    var lifecycle = new VerticleLifecycle(mockVertx, mockSetupService, mockInjector);

    lifecycle.shutDown();
  }

  @Test
  public void shutDown_closeFails_logsError() {
    var mockVertx = mock(Vertx.class);
    when(mockVertx.close()).thenReturn(io.vertx.core.Future.failedFuture("Close failed"));
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(mockVertx, mockSetupService, mockInjector);

    lifecycle.shutDown();
  }

  @Test
  public void shutDown_interrupted_logsWarning() {
    var mockVertx = mock(Vertx.class);
    // Never complete the future so latch.await() blocks
    var promise = io.vertx.core.Promise.<Void>promise();
    when(mockVertx.close()).thenReturn(promise.future());
    var mockSetupService = mock(VerticleSetupService.class);
    var mockInjector = mock(Injector.class);
    var lifecycle = new VerticleLifecycle(mockVertx, mockSetupService, mockInjector);

    Thread.currentThread().interrupt();
    try {
      lifecycle.shutDown();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      // Clear interrupt status for other tests
      Thread.interrupted();
    }
  }

  static final class TestVerticle extends AbstractVerticle {}
}
