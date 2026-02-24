package com.larpconnect.init;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import java.util.concurrent.Callable;

/** VerticleFactory that uses Guice for dependency injection. */
final class GuiceVerticleFactory implements VerticleFactory {
  private final Injector injector;

  @Inject
  GuiceVerticleFactory(Injector injector) {
    this.injector = injector;
  }

  @Override
  public String prefix() {
    return "guice";
  }

  @Override
  @SuppressWarnings({"deprecation", "checkstyle:IllegalCatch"})
  public void createVerticle(
      String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
    String className = VerticleFactory.removePrefix(verticleName);
    try {
      Class<?> clazz = classLoader.loadClass(className);
      promise.complete(() -> (Verticle) injector.getProvider(clazz).get());
    } catch (ReflectiveOperationException | RuntimeException e) {
      promise.fail(e);
    }
  }
}
