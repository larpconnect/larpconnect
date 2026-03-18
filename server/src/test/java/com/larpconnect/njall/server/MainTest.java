package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service.State;
import com.google.inject.AbstractModule;
import com.google.inject.util.Modules;
import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.proto.LarpconnectConfig;
import io.vertx.core.Verticle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class MainTest {

  @Test
  void main_startsAndStopsServerSuccessfully() {
    var runtime = Runtime.getRuntime();
    var overrideModule =
        Modules.override(new ServerModule())
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(LarpconnectConfig.class)
                        .toInstance(
                            LarpconnectConfig.newBuilder()
                                .setWebPort(0)
                                .setOpenapiSpec("openapi.yaml")
                                .build());
                  }
                });
    var main = new Main(runtime, overrideModule);
    var service = main.run();

    assertThat(service.state()).isEqualTo(State.RUNNING);

    main.shutdown(service);

    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  @Test
  void shutdown_handlesExceptions() {
    var runtime = Runtime.getRuntime();
    var overrideModule =
        Modules.override(new ServerModule())
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(LarpconnectConfig.class)
                        .toInstance(
                            LarpconnectConfig.newBuilder()
                                .setWebPort(0)
                                .setOpenapiSpec("openapi.yaml")
                                .build());
                  }
                });
    var main = new Main(runtime, overrideModule);

    var timeoutService = new TestVerticleService();
    timeoutService.exceptionToThrow.set(new RuntimeException("Simulated exception in shutDown"));

    // Test RuntimeException thrown from stopAsync() process
    assertThatCode(() -> main.shutdown(timeoutService)).doesNotThrowAnyException();

    var runtimeService = new TestVerticleService();
    runtimeService.exceptionToThrow.set(new IllegalStateException("Another exception"));
    assertThatCode(() -> main.shutdown(runtimeService)).doesNotThrowAnyException();
  }

  @Test
  void shutdown_handlesTimeoutException() {
    var runtime = Runtime.getRuntime();
    var overrideModule =
        Modules.override(new ServerModule())
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(LarpconnectConfig.class)
                        .toInstance(
                            LarpconnectConfig.newBuilder()
                                .setWebPort(0)
                                .setOpenapiSpec("openapi.yaml")
                                .build());
                  }
                });
    var main = new Main(runtime, overrideModule);

    // Specifically test TimeoutException using a proxy interface implementation
    var timeoutProxyService =
        new VerticleService() {
          @Override
          public void deploy(Class<? extends Verticle> verticleClass) {}

          @Override
          public com.google.common.util.concurrent.Service startAsync() {
            return this;
          }

          @Override
          public boolean isRunning() {
            return true;
          }

          @Override
          public State state() {
            return State.RUNNING;
          }

          @Override
          public com.google.common.util.concurrent.Service stopAsync() {
            return this;
          }

          @Override
          public void awaitRunning() {}

          @Override
          public void awaitRunning(long timeout, TimeUnit unit) {}

          @Override
          public void awaitTerminated() {}

          @Override
          public void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
            throw new TimeoutException("Simulated timeout during shutdown");
          }

          @Override
          public Throwable failureCause() {
            return null;
          }

          @Override
          public void addListener(Listener listener, java.util.concurrent.Executor executor) {}
        };

    assertThatCode(() -> main.shutdown(timeoutProxyService)).doesNotThrowAnyException();
  }

  private static final class TestVerticleService extends AbstractIdleService
      implements VerticleService {
    final AtomicReference<Exception> exceptionToThrow = new AtomicReference<>();
    final AtomicBoolean stopCalled = new AtomicBoolean(false);

    @Override
    public void deploy(Class<? extends Verticle> verticleClass) {}

    @Override
    protected void startUp() throws Exception {}

    @Override
    protected void shutDown() throws Exception {
      stopCalled.set(true);
      Exception ex = exceptionToThrow.get();
      if (ex != null) {
        throw ex;
      }
    }
  }
}
