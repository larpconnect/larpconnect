package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  static final class TestVerticle extends AbstractVerticle {
    final AtomicBoolean started = new AtomicBoolean(false);
    final AtomicBoolean stopped = new AtomicBoolean(false);

    @Override
    public void start() {
      started.set(true);
    }

    @Override
    public void stop() {
      stopped.set(true);
    }
  }

  @Test
  void mainVerticle_startsAndStopsChildren(Vertx vertx, VertxTestContext testContext) {
    TestVerticle testVerticle = new TestVerticle();

    Injector injector =
        Guice.createInjector(
            Modules.override(new ServerModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bindConstant().annotatedWith(Names.named("web.port")).to(0);
                      }
                    }),
            new AbstractModule() {
              @Override
              protected void configure() {
                var binder = Multibinder.newSetBinder(binder(), Verticle.class);
                binder.addBinding().toInstance(testVerticle);
              }
            });

    MainVerticle mainVerticle = injector.getInstance(MainVerticle.class);

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  assertThat(testVerticle.started).isTrue();

                  vertx
                      .undeploy(id)
                      .onComplete(
                          testContext.succeeding(
                              v -> {
                                assertThat(testVerticle.stopped).isTrue();
                                testContext.completeNow();
                              }));
                }));
  }

  @Test
  void mainVerticle_failsIfChildFails(Vertx vertx, VertxTestContext testContext) {
    // We can directly instantiate DefaultMainVerticle to test the failure logic more precisely
    // without relying on Guice for this specific test case, ensuring branch coverage.
    Verticle failingVerticle =
        new AbstractVerticle() {
          @Override
          public void start() {
            throw new RuntimeException("Fail");
          }
        };

    DefaultMainVerticle mainVerticle = new DefaultMainVerticle(ImmutableSet.of(failingVerticle));

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(
            testContext.failing(
                err -> {
                  testContext.verify(
                      () -> {
                        assertThat(err.getMessage()).isEqualTo("Fail");
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void mainVerticle_successPathMocked(Vertx vertx, VertxTestContext testContext) {
    // Mock Vertx to return a known Future for deployVerticle
    // This allows us to verify the DefaultMainVerticle callbacks without actually deploying anything
    Vertx mockVertx = mock(Vertx.class);
    when(mockVertx.deployVerticle(any(Verticle.class)))
        .thenReturn(Future.succeededFuture("mock-id"));

    // We need a Context for init()
    Context mockContext = mock(Context.class);

    Verticle child = new TestVerticle();
    DefaultMainVerticle mainVerticle = new DefaultMainVerticle(ImmutableSet.of(child));

    // Manually init since we are bypassing standard deployment
    mainVerticle.init(mockVertx, mockContext);

    // Call start manually
    Promise<Void> startPromise = Promise.promise();
    mainVerticle.start(startPromise);

    startPromise
        .future()
        .onComplete(
            testContext.succeeding(
                v -> {
                  // Verify that onDeploymentSuccess was effectively covered
                  // But since we are testing implementation details by "trusting" the mock,
                  // let's explicitly call onDeploymentSuccess to ensure it works in isolation.
                  mainVerticle.onDeploymentSuccess(child, "mock-id-2");
                  assertThat(mainVerticle.deploymentIds).containsKey(child);
                  assertThat(mainVerticle.deploymentIds.get(child)).isEqualTo("mock-id-2");

                  // Now verify stop logic coverage
                  Promise<Void> stopPromise = Promise.promise();
                  mainVerticle.stop(stopPromise);
                  stopPromise
                      .future()
                      .onComplete(
                          testContext.succeeding(
                              v2 -> {
                                assertThat(mainVerticle.deploymentIds).isEmpty();
                                testContext.completeNow();
                              }));
                }));
  }

  @Test
  void mainVerticle_emptySet_succeeds(Vertx vertx, VertxTestContext testContext) {
    DefaultMainVerticle mainVerticle = new DefaultMainVerticle(ImmutableSet.of());

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  testContext.completeNow();
                }));
  }
}
