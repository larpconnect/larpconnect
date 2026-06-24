package org.larpconnect.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Promise;
import org.junit.jupiter.api.Test;

/** Unit tests for ApiVerticle. */
public final class ApiVerticleTest {
  @Test
  public void start_whenInvoked_completesPromise() {
    ApiVerticle verticle = new ApiVerticle();
    Promise<Void> promise = Promise.promise();
    verticle.start(promise);
    assertThat(promise.future().succeeded()).isTrue();
  }

  @Test
  public void testApiModule_bindings() {
    com.google.inject.Injector injector =
        com.google.inject.Guice.createInjector(
            new org.larpconnect.events.EventsModule(), new ApiModule());
    java.util.Set<org.larpconnect.events.VerticleProvider> providers =
        injector.getInstance(
            com.google.inject.Key.get(
                new com.google.inject.TypeLiteral<
                    java.util.Set<org.larpconnect.events.VerticleProvider>>() {}));
    assertThat(providers).isNotEmpty();

    org.larpconnect.events.VerticleProvider provider = providers.iterator().next();
    io.vertx.core.Verticle verticle = provider.get();
    assertThat(verticle).isInstanceOf(ApiVerticle.class);
  }
}
