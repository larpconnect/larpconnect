package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import java.util.Collections;
import org.junit.jupiter.api.Test;

final class VerticleServicesTest {

  @Test
  void create_emptyModules_returnsVerticleLifecycle() {
    VerticleService service = VerticleServices.create(Collections.emptyList());

    assertThat(service).isNotNull();
    assertThat(service).isInstanceOf(VerticleLifecycle.class);
  }

  @Test
  void create_withVertxAndFactory_returnsVerticleLifecycle() {
    Vertx vertx = mock(Vertx.class);
    VerticleFactory verticleFactory = mock(VerticleFactory.class);

    VerticleService service = VerticleServices.create(vertx, verticleFactory);

    assertThat(service).isNotNull();
    assertThat(service).isInstanceOf(VerticleLifecycle.class);
  }

  @Test
  void create_withVertxAndNullFactory_returnsVerticleLifecycle() {
    Vertx vertx = mock(Vertx.class);

    VerticleService service = VerticleServices.create(vertx, null);

    assertThat(service).isNotNull();
    assertThat(service).isInstanceOf(VerticleLifecycle.class);
  }
}
