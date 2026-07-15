package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.larpconnect.data.DatabaseMigrator;
import org.larpconnect.events.MainVerticle;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the ServerService lifecycle manager. */
@ExtendWith(MockitoExtension.class)
public final class ServerServiceTest {
  @Mock private DatabaseMigrator mockInitializer;
  @Mock private Vertx vertx;
  @Mock private MainVerticle mainVerticle;

  private ServerService service;

  @BeforeEach
  public void setUp() {
    service = new ServerService(() -> vertx, () -> mainVerticle, mockInitializer);
  }

  @Test
  public void startUp_deploysMainVerticle() throws Exception {
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close()).thenReturn(Future.succeededFuture());

    service.startAsync().awaitRunning();

    try {
      assertThat(service.getDeploymentId()).isEqualTo("deploymentId");
      verify(vertx).deployVerticle(mainVerticle);
      verify(mockInitializer).migrate();
    } finally {
      service.stopAsync().awaitTerminated();
    }
  }

  @Test
  public void shutDown_closesVertx() throws Exception {
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close()).thenReturn(Future.succeededFuture());

    service.startAsync().awaitRunning();
    service.stopAsync().awaitTerminated();

    verify(vertx).close();
  }

  @Test
  public void startUp_failure_throwsException() {
    when(vertx.deployVerticle(mainVerticle))
        .thenReturn(Future.failedFuture(new RuntimeException("Simulated deploy error")));

    assertThatThrownBy(() -> service.startAsync().awaitRunning())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void shutDown_failure_throwsException() throws Exception {
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close())
        .thenReturn(Future.failedFuture(new RuntimeException("Simulated close error")));

    service.startAsync().awaitRunning();

    assertThatThrownBy(() -> service.stopAsync().awaitTerminated())
        .isInstanceOf(IllegalStateException.class);
  }
}
