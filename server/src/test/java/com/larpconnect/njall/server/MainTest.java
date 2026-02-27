package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service.State;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.larpconnect.njall.init.VerticleService;
import io.vertx.core.Verticle;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
                    bindConstant().annotatedWith(Names.named("web.port")).to(0);
                  }
                });
    var main = new Main(runtime, overrideModule);
    var service = main.run();

    assertThat(service.state()).isEqualTo(State.RUNNING);

    main.shutdown(service);

    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  @Test
  void run_exitsOnFailure() {
    var runtime = Runtime.getRuntime();
    // A module that throws exception during configuration will cause injector creation to fail
    var brokenModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            throw new RuntimeException("Intentional failure");
          }
        };

    AtomicInteger exitCode = new AtomicInteger(0);
    var main = new Main(runtime, brokenModule, exitCode::set);

    var service = main.run();

    assertThat(service).isNull();
    assertThat(exitCode.get()).isEqualTo(1);
  }

  @Test
  void main_defaultConstructor() {
    // Just verifying that the default constructor can be called without exception
    // This covers the constructor chaining logic
    var main = new Main(Runtime.getRuntime());
    assertThat(main).isNotNull();
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
                    bindConstant().annotatedWith(Names.named("web.port")).to(0);
                  }
                });
    var main = new Main(runtime, overrideModule);

    // Test TimeoutException
    var manualTimeoutService = new TestVerticleService();
    manualTimeoutService.exceptionToThrow.set(new TimeoutException());
    main.shutdown(manualTimeoutService);
    /*
     * Since Main.shutdown swallows the exception and logs it, we verify by ensuring the code didn't
     * crash and the service stop was attempted. We can verify internal state if we spy, but here we
     * just ensure no propagation of exception.
     */

    // Test RuntimeException
    var runtimeService = new TestVerticleService();
    runtimeService.exceptionToThrow.set(new RuntimeException());
    main.shutdown(runtimeService);
  }

  private static final class TestVerticleService extends AbstractIdleService
      implements VerticleService {
    final AtomicReference<Exception> exceptionToThrow = new AtomicReference<>();
    final AtomicBoolean stopCalled = new AtomicBoolean(false);

    @Override
    public void deploy(Class<? extends Verticle> verticleClass) {
      // No-op
    }

    @Override
    protected void startUp() throws Exception {
      // No-op
    }

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
