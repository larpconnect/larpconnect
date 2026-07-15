package org.larpconnect.events;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import io.vertx.core.Deployable;
import io.vertx.core.Promise;
import io.vertx.core.spi.VerticleFactory;
import java.util.concurrent.Callable;

/** A VerticleFactory that uses Guice Injector to instantiate verticles. */
public final class GuiceVerticleFactory implements VerticleFactory {
  public static final String PREFIX = "guice";

  private final Provider<Injector> injectorProvider;

  @Inject
  public GuiceVerticleFactory(Provider<Injector> injectorProvider) {
    this.injectorProvider = injectorProvider;
  }

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void createVerticle2(
      String verticleName,
      ClassLoader classLoader,
      Promise<Callable<? extends Deployable>> promise) {
    String className = VerticleFactory.removePrefix(verticleName);
    try {
      Class<?> clazz = classLoader.loadClass(className);
      promise.complete(() -> (Deployable) injectorProvider.get().getInstance(clazz));
    } catch (Exception e) {
      promise.fail(e);
    }
  }
}
