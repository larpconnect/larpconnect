package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.DatabaseInitializer;

/** Unit tests for ServerApp verifying main method startup. */
public final class ServerAppTest {
  @Test
  public void testMainMethod_startsAndStops() throws Exception {
    Module mockModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(DatabaseInitializer.class).toInstance(mock(DatabaseInitializer.class));
          }
        };

    ServerApp.setOverrideModule(mockModule);
    try {
      ServerApp.main(new String[0]);

      ServerService service = ServerApp.getRunningService();
      assertThat(service).isNotNull();
      assertThat(service.isRunning()).isTrue();

      service.stopAsync().awaitTerminated();
      assertThat(service.isRunning()).isFalse();
    } finally {
      ServerApp.setOverrideModule(null);
    }
  }

  @Test
  public void testShutdownHookRunnable_success() {
    io.vertx.core.Vertx mockVertx = mock(io.vertx.core.Vertx.class);
    when(mockVertx.close()).thenReturn(io.vertx.core.Future.succeededFuture());
    org.larpconnect.events.MainVerticle mockMain = mock(org.larpconnect.events.MainVerticle.class);
    when(mockVertx.deployVerticle(mockMain))
        .thenReturn(io.vertx.core.Future.succeededFuture("deploymentId"));

    DatabaseInitializer mockInitializer = mock(DatabaseInitializer.class);
    ServerService service = new ServerService(() -> mockVertx, () -> mockMain, mockInitializer);
    service.startAsync().awaitRunning();

    Runnable hook = ServerApp.createShutdownHookRunnable(service);
    hook.run();

    assertThat(service.state())
        .isEqualTo(com.google.common.util.concurrent.Service.State.TERMINATED);
  }

  @Test
  public void testShutdownHookRunnable_failure() {
    io.vertx.core.Vertx mockVertx = mock(io.vertx.core.Vertx.class);
    when(mockVertx.close())
        .thenReturn(
            io.vertx.core.Future.failedFuture(new RuntimeException("Simulated shutdown error")));
    org.larpconnect.events.MainVerticle mockMain = mock(org.larpconnect.events.MainVerticle.class);
    when(mockVertx.deployVerticle(mockMain))
        .thenReturn(io.vertx.core.Future.succeededFuture("deploymentId"));

    DatabaseInitializer mockInitializer = mock(DatabaseInitializer.class);
    ServerService service = new ServerService(() -> mockVertx, () -> mockMain, mockInitializer);
    service.startAsync().awaitRunning();

    Runnable hook = ServerApp.createShutdownHookRunnable(service);
    hook.run();

    assertThat(service.state()).isEqualTo(com.google.common.util.concurrent.Service.State.FAILED);
  }
}
