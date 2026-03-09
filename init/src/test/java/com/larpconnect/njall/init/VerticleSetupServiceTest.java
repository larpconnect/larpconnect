package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import com.larpconnect.njall.common.codec.ProtoCodec;
import com.larpconnect.njall.proto.MessageRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class VerticleSetupServiceTest {

  private Vertx mockVertx;
  private EventBus mockEventBus;
  private Injector mockInjector;
  private VerticleSetupService service;

  @BeforeEach
  void setUp() {
    mockVertx = mock(Vertx.class);
    mockEventBus = mock(EventBus.class);
    mockInjector = mock(Injector.class);
    when(mockVertx.eventBus()).thenReturn(mockEventBus);
    when(mockEventBus.registerDefaultCodec(any(), any())).thenReturn(mockEventBus);
    service = new VerticleSetupService(mock(ProtoCodec.class));
  }

  @Test
  void deploy_notSetup_throwsException() {
    assertThatThrownBy(() -> service.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Vertx not initialized");
  }

  @Test
  void deploy_success_logsInfo() {
    when(mockVertx.deployVerticle(anyString())).thenReturn(Future.succeededFuture("id"));

    service.setup(mockVertx, mockInjector);
    service.deploy(TestVerticle.class);

    verify(mockVertx).deployVerticle("guice:" + TestVerticle.class.getName());
    verify(mockEventBus).registerDefaultCodec(eq(MessageRequest.class), any());
  }

  @Test
  void deploy_failure_throwsIllegalStateException() {
    var failure = new IllegalStateException("fail");
    when(mockVertx.deployVerticle(anyString())).thenReturn(Future.failedFuture(failure));

    service.setup(mockVertx, mockInjector);

    assertThatThrownBy(() -> service.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to deploy verticle")
        .hasCause(failure);
  }

  static final class TestVerticle extends AbstractVerticle {}
}
