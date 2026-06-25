package org.larpconnect.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for ServerApp verifying main method startup. */
public final class ServerAppTest {
  @Test
  @org.junit.jupiter.api.Disabled("Requires a running database and schema migrations")
  public void testMainMethod_startsAndStops() throws Exception {
    ServerApp.main(new String[0]);

    ServerService service = ServerApp.getRunningService();
    assertThat(service).isNotNull();
    assertThat(service.isRunning()).isTrue();

    service.stopAsync().awaitTerminated();
    assertThat(service.isRunning()).isFalse();
  }

  @Test
  public void testShutdownHookRunnable_success() {
    io.vertx.core.Vertx mockVertx = org.mockito.Mockito.mock(io.vertx.core.Vertx.class);
    org.mockito.Mockito.when(mockVertx.close()).thenReturn(io.vertx.core.Future.succeededFuture());
    org.larpconnect.events.MainVerticle mockMain =
        org.mockito.Mockito.mock(org.larpconnect.events.MainVerticle.class);
    org.mockito.Mockito.when(mockVertx.deployVerticle(mockMain))
        .thenReturn(io.vertx.core.Future.succeededFuture("deploymentId"));

    org.larpconnect.data.hibernate.HibernateService mockHibernate = new DummyHibernateService();

    ServerService service = new ServerService(() -> mockVertx, () -> mockMain, mockHibernate);
    service.startAsync().awaitRunning();

    Runnable hook = ServerApp.createShutdownHookRunnable(service);
    hook.run();

    assertThat(service.state())
        .isEqualTo(com.google.common.util.concurrent.Service.State.TERMINATED);
  }

  @Test
  public void testShutdownHookRunnable_failure() {
    io.vertx.core.Vertx mockVertx = org.mockito.Mockito.mock(io.vertx.core.Vertx.class);
    org.mockito.Mockito.when(mockVertx.close())
        .thenReturn(
            io.vertx.core.Future.failedFuture(new RuntimeException("Simulated shutdown error")));
    org.larpconnect.events.MainVerticle mockMain =
        org.mockito.Mockito.mock(org.larpconnect.events.MainVerticle.class);
    org.mockito.Mockito.when(mockVertx.deployVerticle(mockMain))
        .thenReturn(io.vertx.core.Future.succeededFuture("deploymentId"));

    org.larpconnect.data.hibernate.HibernateService mockHibernate = new DummyHibernateService();

    ServerService service = new ServerService(() -> mockVertx, () -> mockMain, mockHibernate);
    service.startAsync().awaitRunning();

    Runnable hook = ServerApp.createShutdownHookRunnable(service);
    hook.run();

    assertThat(service.state()).isEqualTo(com.google.common.util.concurrent.Service.State.FAILED);
  }

  private static final class DummyHibernateService
      extends com.google.common.util.concurrent.AbstractIdleService
      implements org.larpconnect.data.hibernate.HibernateService {
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
