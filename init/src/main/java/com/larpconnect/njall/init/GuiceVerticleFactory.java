package com.larpconnect.njall.init;

import com.google.inject.Injector;
import io.vertx.core.Deployable;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import java.util.concurrent.Callable;

final class GuiceVerticleFactory implements VerticleFactory {

  private final Injector injector;

  GuiceVerticleFactory(Injector injector) {
    this.injector = injector;
  }

  @Override
  public String prefix() {
    return "guice";
  }

  @Override
  public void createVerticle2(
      String verticleName,
      ClassLoader classLoader,
      Promise<Callable<? extends Deployable>> promise) {
    var clazzName = VerticleFactory.removePrefix(verticleName);
    promise.complete(
        () -> {
          try {
            var clazz = classLoader.loadClass(clazzName);
            return (Verticle) injector.getInstance(clazz);
          } catch (ClassNotFoundException | ClassCastException e) {
            throw new IllegalArgumentException("Failed to load verticle class: " + clazzName, e);
          }
        });
  }
}
