package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Service.State;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.DatabaseMigrator;
import org.larpconnect.events.MainVerticle;

/** Unit tests for ServerApp verifying main method startup. */
public final class ServerAppTest {
  @Test
  public void testMainMethod_startsAndStops() throws Exception {
    Module mockModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(DatabaseMigrator.class).toInstance(mock(DatabaseMigrator.class));
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
    Vertx mockVertx = mock(Vertx.class);
    when(mockVertx.close()).thenReturn(Future.succeededFuture());
    MainVerticle mockMain = mock(MainVerticle.class);
    when(mockVertx.deployVerticle(mockMain)).thenReturn(Future.succeededFuture("deploymentId"));

    DatabaseMigrator mockInitializer = mock(DatabaseMigrator.class);
    ServerService service = new ServerService(() -> mockVertx, () -> mockMain, mockInitializer);
    service.startAsync().awaitRunning();

    Runnable hook = ServerApp.createShutdownHookRunnable(service);
    hook.run();

    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  @Test
  public void testShutdownHookRunnable_failure() {
    Vertx mockVertx = mock(Vertx.class);
    when(mockVertx.close())
        .thenReturn(Future.failedFuture(new RuntimeException("Simulated shutdown error")));
    MainVerticle mockMain = mock(MainVerticle.class);
    when(mockVertx.deployVerticle(mockMain)).thenReturn(Future.succeededFuture("deploymentId"));

    DatabaseMigrator mockInitializer = mock(DatabaseMigrator.class);
    ServerService service = new ServerService(() -> mockVertx, () -> mockMain, mockInitializer);
    service.startAsync().awaitRunning();

    Runnable hook = ServerApp.createShutdownHookRunnable(service);
    hook.run();

    assertThat(service.state()).isEqualTo(State.FAILED);
  }
}
