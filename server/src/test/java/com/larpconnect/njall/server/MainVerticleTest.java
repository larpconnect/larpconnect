package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.AbstractVerticle;
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
            new ServerModule(),
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
}
