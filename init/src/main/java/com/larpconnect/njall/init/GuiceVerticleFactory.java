package com.larpconnect.njall.init;

import com.google.inject.Injector;
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
  @SuppressWarnings("deprecation")
  public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
    String clazzName = VerticleFactory.removePrefix(verticleName);
    promise.complete(() -> {
      try {
        Class<?> clazz = classLoader.loadClass(clazzName);
        return (Verticle) injector.getInstance(clazz);
      } catch (ClassNotFoundException | ClassCastException e) {
        throw new RuntimeException("Failed to load verticle class: " + clazzName, e);
      } catch (Exception e) {
        // Handle reflective operation exceptions or Guice instantiation errors
        if (e instanceof ReflectiveOperationException) {
           throw new RuntimeException("Reflective error instantiating verticle: " + clazzName, e);
        }
        throw new RuntimeException("Failed to instantiate verticle: " + clazzName, e);
      }
    });
  }
}
