package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class VerticleLifecycleTest {

  @Test
  public void startUp_validConfig_success() throws Exception {
    VerticleService lifecycle = VerticleServices.create(Collections.emptyList());

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isTrue();

    Vertx vertx = lifecycle.getVertx();
    assertThat(vertx).isNotNull();

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }
}
