package com.larpconnect.init;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import java.util.concurrent.Callable;

/** VerticleFactory that uses Guice for dependency injection. */
public class GuiceVerticleFactory implements VerticleFactory {
  private final Injector injector;

  @Inject
  public GuiceVerticleFactory(Injector injector) {
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
      // We assume the class implements Verticle, but we can't cast until we instantiate if we want
      // to be safe.
      // However, Guice getInstance usually returns the type of the class.
      // But we need to return Callable<Verticle>.

      promise.complete(() -> (Verticle) injector.getInstance(clazz));
    } catch (ReflectiveOperationException | RuntimeException e) {
      promise.fail(e);
    }
  }
}
