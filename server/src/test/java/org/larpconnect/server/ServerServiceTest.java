package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.hibernate.HibernateService;
import org.larpconnect.events.MainVerticle;

/** Unit tests for the ServerService lifecycle manager. */
public final class ServerServiceTest {
  private HibernateService mockHibernate;

  @BeforeEach
  public void setUp() {
    mockHibernate = new DummyHibernateService();
  }

  @Test
  public void startUp_deploysMainVerticle() throws Exception {
    Vertx vertx = mock(Vertx.class);
    MainVerticle mainVerticle = mock(MainVerticle.class);
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close()).thenReturn(Future.succeededFuture());

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockHibernate);
    service.startAsync().awaitRunning();

    try {
      assertThat(service.getDeploymentId()).isEqualTo("deploymentId");
      verify(vertx).deployVerticle(mainVerticle);
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

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockHibernate);
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

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockHibernate);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.startAsync().awaitRunning())
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(RuntimeException.class);
  }

  @Test
  public void shutDown_failure_throwsException() throws Exception {
    Vertx vertx = mock(Vertx.class);
    MainVerticle mainVerticle = mock(MainVerticle.class);
    when(vertx.deployVerticle(mainVerticle)).thenReturn(Future.succeededFuture("deploymentId"));
    when(vertx.close())
        .thenReturn(Future.failedFuture(new RuntimeException("Simulated close error")));

    ServerService service = new ServerService(() -> vertx, () -> mainVerticle, mockHibernate);
    service.startAsync().awaitRunning();

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.stopAsync().awaitTerminated())
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(RuntimeException.class);
  }

  private static final class DummyHibernateService
      extends com.google.common.util.concurrent.AbstractIdleService implements HibernateService {
    @Override
    public org.hibernate.SessionFactory getSessionFactory() {
      return null;
    }

    @Override
    protected void startUp() {}

    @Override
    protected void shutDown() {}
  }
}
