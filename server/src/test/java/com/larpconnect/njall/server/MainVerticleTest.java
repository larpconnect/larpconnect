package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.time.TimeService;
import com.larpconnect.njall.proto.LarpConnectConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class MainVerticleTest {

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
    var testVerticle = new TestVerticle();

    var injector =
        Guice.createInjector(
            Modules.override(new ServerModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(LarpConnectConfig.class)
                            .toInstance(
                                LarpConnectConfig.newBuilder()
                                    .setWebPort(0)
                                    .setOpenapiSpec("openapi.yaml")
                                    .build());
                      }
                    }),
            new AbstractModule() {
              @Override
              protected void configure() {
                var binder = Multibinder.newSetBinder(binder(), Verticle.class);
                binder.addBinding().toInstance(testVerticle);
              }
            });

    TimeService timeService = injector.getInstance(TimeService.class);
    timeService.startAsync().awaitRunning();

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
    var injector =
        Guice.createInjector(
            Modules.override(new ServerModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(LarpConnectConfig.class)
                            .toInstance(
                                LarpConnectConfig.newBuilder()
                                    .setWebPort(0)
                                    .setOpenapiSpec("openapi.yaml")
                                    .build());
                      }
                    }),
            new AbstractModule() {
              @Override
              protected void configure() {
                var binder = Multibinder.newSetBinder(binder(), Verticle.class);
                binder
                    .addBinding()
                    .toInstance(
                        new AbstractVerticle() {
                          @Override
                          public void start() {
                            throw new IllegalStateException("Fail");
                          }
                        });
              }
            });

    TimeService timeService = injector.getInstance(TimeService.class);
    timeService.startAsync().awaitRunning();

    MainVerticle mainVerticle = injector.getInstance(MainVerticle.class);

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(testContext.failing(err -> testContext.completeNow()));
  }
}
