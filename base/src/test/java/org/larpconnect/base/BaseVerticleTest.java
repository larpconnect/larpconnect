package org.larpconnect.base;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Promise;
import org.junit.jupiter.api.Test;

/** Unit tests for BaseVerticle. */
public final class BaseVerticleTest {
  @Test
  public void start_whenInvoked_completesPromise() {
    BaseVerticle verticle = new BaseVerticle();
    Promise<Void> promise = Promise.promise();
    verticle.start(promise);
    assertThat(promise.future().succeeded()).isTrue();
  }

  @Test
  public void testBaseModule_bindings() {
    com.google.inject.Injector injector =
        com.google.inject.Guice.createInjector(
            new org.larpconnect.events.EventsModule(), new BaseModule());
    java.util.Set<org.larpconnect.events.VerticleProvider> providers =
        injector.getInstance(
            com.google.inject.Key.get(
                new com.google.inject.TypeLiteral<
                    java.util.Set<org.larpconnect.events.VerticleProvider>>() {}));
    assertThat(providers).isNotEmpty();

    org.larpconnect.events.VerticleProvider provider = providers.iterator().next();
    io.vertx.core.Verticle verticle = provider.get();
    assertThat(verticle).isInstanceOf(BaseVerticle.class);
  }
}
