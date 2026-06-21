package org.larpconnect.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.Promise;
import org.junit.jupiter.api.Test;

/** Unit tests for events infrastructure configuration. */
public final class EventsModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new EventsModule());
    assertThat(injector).isNotNull();
  }

  @Test
  public void mainVerticle_instantiation_isNotNull() {
    MainVerticle verticle = new MainVerticle();
    assertThat(verticle).isNotNull();
  }

  @Test
  public void start_withPromise_succeeds() {
    MainVerticle verticle = new MainVerticle();
    Promise<Void> promise = Promise.promise();
    verticle.start(promise);
    assertThat(promise.future().succeeded()).isTrue();
  }
}
