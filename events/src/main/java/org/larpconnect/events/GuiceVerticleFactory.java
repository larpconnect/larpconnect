package org.larpconnect.events;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import java.util.concurrent.Callable;

/** A VerticleFactory that uses Guice Injector to instantiate verticles. */
public final class GuiceVerticleFactory implements VerticleFactory {
  private final Provider<Injector> injectorProvider;

  @Inject
  public GuiceVerticleFactory(Provider<Injector> injectorProvider) {
    this.injectorProvider = injectorProvider;
  }

  @Override
  public String prefix() {
    return "java-guice";
  }

  @Override
  @SuppressWarnings("deprecation")
  public void createVerticle(
      String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
    String className = VerticleFactory.removePrefix(verticleName);
    try {
      Class<?> clazz = classLoader.loadClass(className);
      promise.complete(() -> (Verticle) injectorProvider.get().getInstance(clazz));
    } catch (Exception e) {
      promise.fail(e);
    }
  }
}
