package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import io.vertx.core.AbstractVerticle;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BootstrapVerticleServiceTest {

  @Test
  public void deploy_notStarted_throwsException() {
    var lifecycle = new BootstrapVerticleService(ImmutableList.of());
    assertThatThrownBy(() -> lifecycle.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("BootstrapVerticleService not started");
  }

  @Test
  public void startUp_validConfig_success() throws Exception {
    var lifecycle =
        new BootstrapVerticleService(
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
      var lifecycle = new BootstrapVerticleService(ImmutableList.of());
      assertThatThrownBy(() -> lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("BootstrapVerticleService [FAILED]");
      assertThat(lifecycle.failureCause())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Failed to load default config")
          .hasCauseInstanceOf(IllegalArgumentException.class);
    } finally {
      System.clearProperty("njall.config.resource");
    }
  }

  static final class TestVerticle extends AbstractVerticle {}
}
