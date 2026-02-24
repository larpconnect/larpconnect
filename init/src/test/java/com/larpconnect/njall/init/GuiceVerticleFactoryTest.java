package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class GuiceVerticleFactoryTest {

  public static class MyVerticle extends AbstractVerticle {}

  @Test
  public void testCreateVerticle(VertxTestContext testContext) {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MyVerticle.class).asEagerSingleton();
      }
    });

    GuiceVerticleFactory factory = new GuiceVerticleFactory(injector);
    assertThat(factory.prefix()).isEqualTo("guice");

    Promise<Callable<Verticle>> promise = Promise.promise();
    factory.createVerticle("guice:" + MyVerticle.class.getName(), getClass().getClassLoader(), promise);

    promise.future().onComplete(testContext.succeeding(callable -> {
      try {
        Verticle verticle = callable.call();
        assertThat(verticle).isInstanceOf(MyVerticle.class);
        testContext.completeNow();
      } catch (Exception e) {
        testContext.failNow(e);
      }
    }));
  }
}
