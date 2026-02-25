package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Deployable;
import io.vertx.core.Promise;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class GuiceVerticleFactoryTest {

  public static class TestVerticle extends AbstractVerticle {}

  @Test
  public void createVerticle_validClass_success(VertxTestContext testContext) {
    Injector injector =
        Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(TestVerticle.class).in(Scopes.SINGLETON);
              }
            });

    GuiceVerticleFactory factory = new GuiceVerticleFactory(injector);
    assertThat(factory.prefix()).isEqualTo("guice");

    Promise<Callable<? extends Deployable>> promise = Promise.promise();
    factory.createVerticle2(
        "guice:" + TestVerticle.class.getName(), getClass().getClassLoader(), promise);

    promise
        .future()
        .onComplete(
            testContext.succeeding(
                callable -> {
                  try {
                    Deployable verticle = callable.call();
                    assertThat(verticle).isInstanceOf(TestVerticle.class);
                    testContext.completeNow();
                  } catch (Exception e) { // Callable throws Exception
                    testContext.failNow(e);
                  }
                }));
  }

  @Test
  public void createVerticle_missingClass_failure(VertxTestContext testContext) {
    Injector injector = Guice.createInjector();
    GuiceVerticleFactory factory = new GuiceVerticleFactory(injector);

    Promise<Callable<? extends Deployable>> promise = Promise.promise();
    factory.createVerticle2("guice:com.example.MissingClass", getClass().getClassLoader(), promise);

    promise
        .future()
        .onComplete(
            testContext.succeeding(
                callable -> {
                  try {
                    callable.call();
                    testContext.failNow("Expected IllegalArgumentException was not thrown");
                  } catch (IllegalArgumentException e) {
                    assertThat(e)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Failed to load verticle class");
                    testContext.completeNow();
                  } catch (Exception e) {
                    testContext.failNow(e);
                  }
                }));
  }
}
