package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
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
                binder
                    .addBinding()
                    .toInstance(
                        new AbstractVerticle() {
                          @Override
                          public void start() {
                            throw new RuntimeException("Fail");
                          }
                        });
              }
            });

    MainVerticle mainVerticle = injector.getInstance(MainVerticle.class);

    vertx
        .deployVerticle(mainVerticle)
        .onComplete(testContext.failing(err -> testContext.completeNow()));
  }
}
