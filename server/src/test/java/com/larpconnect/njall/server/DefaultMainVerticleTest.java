package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
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
final class DefaultMainVerticleTest {

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

  static final class FailingVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      startPromise.fail(new IllegalStateException("Fail"));
    }
  }

  @Test
  void start_deploysAllVerticles_successfully(Vertx vertx, VertxTestContext testContext) {
    var testVerticle = new TestVerticle();
    var mainVerticle = new DefaultMainVerticle(ImmutableSet.of(testVerticle));

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  assertThat(testVerticle.started).isTrue();
                  testContext.completeNow();
                }));
  }

  @Test
  void start_failsPromise_whenChildVerticleDeploymentFails(
      Vertx vertx, VertxTestContext testContext) {
    var failingVerticle = new FailingVerticle();
    var mainVerticle = new DefaultMainVerticle(ImmutableSet.of(failingVerticle));

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(testContext.failing(err -> testContext.completeNow()));
  }

  @Test
  void start_failsPromise_whenVertxDeployVerticleFails(VertxTestContext testContext) {
    var testVerticle = new TestVerticle();
    var mainVerticle = new DefaultMainVerticle(ImmutableSet.of(testVerticle));

    var mockVertx = mock(Vertx.class);
    var mockContext = mock(Context.class);

    var failure = new IllegalStateException("Deployment failed");
    when(mockVertx.deployVerticle(any(Verticle.class))).thenReturn(Future.failedFuture(failure));

    mainVerticle.init(mockVertx, mockContext);
    Promise<Void> startPromise = Promise.promise();
    mainVerticle.start(startPromise);

    startPromise
        .future()
        .onComplete(
            testContext.failing(
                err -> {
                  assertThat(err).isSameAs(failure);
                  testContext.completeNow();
                }));
  }

  @Test
  void stop_completesSuccessfully(Vertx vertx, VertxTestContext testContext) {
    var testVerticle = new TestVerticle();
    var mainVerticle = new DefaultMainVerticle(ImmutableSet.of(testVerticle));

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
}
