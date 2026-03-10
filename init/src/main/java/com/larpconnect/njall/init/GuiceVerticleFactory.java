package com.larpconnect.njall.init;

import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.inject.Injector;
import com.larpconnect.njall.common.annotations.AiContract;
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
  @AiContract(ensure = "$res \\equiv \"guice\"", tags = PURE)
  public String prefix() {
    return "guice";
  }

  @Override
  @AiContract(
      require = {"verticleName \\neq \\bot", "classLoader \\neq \\bot", "promise \\neq \\bot"},
      ensure = "promise \\text{ is completed with a Verticle factory}",
      implementationHint =
          "Resolves and instantiates the Verticle using the Guice injector by class name.")
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
