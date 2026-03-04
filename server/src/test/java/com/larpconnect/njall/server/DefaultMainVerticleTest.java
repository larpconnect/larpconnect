package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Set;
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
      startPromise.fail(new RuntimeException("Fail"));
    }
  }

  @Test
  void start_deploysAllVerticles_successfully(Vertx vertx, VertxTestContext testContext) {
    TestVerticle testVerticle = new TestVerticle();
    DefaultMainVerticle mainVerticle = new DefaultMainVerticle(Set.of(testVerticle));

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
    FailingVerticle failingVerticle = new FailingVerticle();
    DefaultMainVerticle mainVerticle = new DefaultMainVerticle(Set.of(failingVerticle));

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(testContext.failing(err -> testContext.completeNow()));
  }

  @Test
  void stop_completesSuccessfully(Vertx vertx, VertxTestContext testContext) {
    TestVerticle testVerticle = new TestVerticle();
    DefaultMainVerticle mainVerticle = new DefaultMainVerticle(Set.of(testVerticle));

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
