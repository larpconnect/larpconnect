package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.AbstractVerticle;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class VerticleLifecycleTest {

  @Test
  public void startUp_validConfig_success() throws Exception {
    var lifecycle = VerticleServices.create(Collections.emptyList());

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
      var lifecycle = VerticleServices.create(Collections.emptyList());
      assertThatThrownBy(() -> lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS))
          .isInstanceOf(IllegalStateException.class);
    } finally {
      System.clearProperty("njall.config.resource");
    }
  }

  @Test
  public void deploy_notStarted_throwsException() {
    var lifecycle = new VerticleLifecycle(Collections.emptyList());
    assertThatThrownBy(() -> lifecycle.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("VerticleLifecycle not started");
  }

  @Test
  public void shutDown_closeNotStarted_doesNothing() {
    var lifecycle = new VerticleLifecycle(Collections.emptyList());
    // Never started, so shutDown should safely do nothing
    lifecycle.shutDown();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shutDown_closeFails_logsError() throws Exception {
    var lifecycle = new VerticleLifecycle(Collections.emptyList());
    var field = VerticleLifecycle.class.getDeclaredField("vertxRef");
    field.setAccessible(true);
    var vertxRef = (AtomicReference<io.vertx.core.Vertx>) field.get(lifecycle);

    var mockVertx = mock(io.vertx.core.Vertx.class);
    when(mockVertx.close()).thenReturn(io.vertx.core.Future.failedFuture("Close failed"));
    vertxRef.set(mockVertx);

    lifecycle.shutDown();

    // We should be able to get here without throwing, and it will have logged an error.
  }

  static final class TestVerticle extends AbstractVerticle {}
}
