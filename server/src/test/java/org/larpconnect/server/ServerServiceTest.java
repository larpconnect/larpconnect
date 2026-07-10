package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.DatabaseInitializer;
import org.larpconnect.events.MainVerticle;

/** Unit tests for the ServerService lifecycle manager. */
public final class ServerServiceTest {
  private DatabaseInitializer mockInitializer;

  @BeforeEach
  public void setUp() {
    mockInitializer = mock(DatabaseInitializer.class);
  }

  @Test
  public void startUp_deploysMainVerticle() throws Exception {
    Vertx vertx = mock(Vertx.class);
    MainVerticle mainVerticle = mock(MainVerticle.class);
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close()).thenReturn(Future.succeededFuture());

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockInitializer);
    service.startAsync().awaitRunning();

    try {
      assertThat(service.getDeploymentId()).isEqualTo("deploymentId");
      verify(vertx).deployVerticle(mainVerticle);
      verify(mockInitializer).migrate();
    } finally {
      // Clean up local mock state if any, though it is just a mock.
      service.stopAsync().awaitTerminated();
    }
  }

  @Test
  public void shutDown_closesVertx() throws Exception {
    Vertx vertx = mock(Vertx.class);
    MainVerticle mainVerticle = mock(MainVerticle.class);
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close()).thenReturn(Future.succeededFuture());

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockInitializer);
    service.startAsync().awaitRunning();
    service.stopAsync().awaitTerminated();

    verify(vertx).close();
  }

  @Test
  public void startUp_failure_throwsException() {
    Vertx vertx = mock(Vertx.class);
    MainVerticle mainVerticle = mock(MainVerticle.class);
    when(vertx.deployVerticle(mainVerticle))
        .thenReturn(Future.failedFuture(new RuntimeException("Simulated deploy error")));

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockInitializer);

    assertThatThrownBy(() -> service.startAsync().awaitRunning())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void shutDown_failure_throwsException() throws Exception {
    Vertx vertx = mock(Vertx.class);
    MainVerticle mainVerticle = mock(MainVerticle.class);
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close())
        .thenReturn(Future.failedFuture(new RuntimeException("Simulated close error")));

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockInitializer);
    service.startAsync().awaitRunning();

    assertThatThrownBy(() -> service.stopAsync().awaitTerminated())
        .isInstanceOf(IllegalStateException.class);
  }
}
