package org.larpconnect.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
    MainVerticle verticle = new MainVerticle(Set.of());
    assertThat(verticle).isNotNull();
  }

  @Test
  public void start_withPromise_succeeds() {
    Vertx vertx = Vertx.vertx();
    try {
      MainVerticle verticle = new MainVerticle(Set.of());
      verticle.init(vertx, vertx.getOrCreateContext());
      Promise<Void> promise = Promise.promise();
      verticle.start(promise);
      assertThat(promise.future().succeeded()).isTrue();
    } finally {
      vertx.close();
    }
  }

  @Test
  public void start_withSucceedingProvider_succeeds() {
    Vertx vertx = Vertx.vertx();
    try {
      VerticleProvider provider = () -> new TestVerticle(true);
      MainVerticle verticle = new MainVerticle(Set.of(provider));
      verticle.init(vertx, vertx.getOrCreateContext());
      Promise<Void> promise = Promise.promise();
      verticle.start(promise);

      CompletableFuture<Void> future = new CompletableFuture<>();
      promise
          .future()
          .onSuccess(v -> future.complete(null))
          .onFailure(future::completeExceptionally);

      future.orTimeout(5, TimeUnit.SECONDS).join();
      assertThat(promise.future().succeeded()).isTrue();
    } finally {
      vertx.close();
    }
  }

  @Test
  public void start_withFailingProvider_fails() {
    Vertx vertx = Vertx.vertx();
    try {
      VerticleProvider provider = () -> new TestVerticle(false);
      MainVerticle verticle = new MainVerticle(Set.of(provider));
      verticle.init(vertx, vertx.getOrCreateContext());
      Promise<Void> promise = Promise.promise();
      verticle.start(promise);

      CompletableFuture<Void> future = new CompletableFuture<>();
      promise
          .future()
          .onSuccess(v -> future.complete(null))
          .onFailure(future::completeExceptionally);

      try {
        future.orTimeout(5, TimeUnit.SECONDS).join();
      } catch (Exception e) {
        // Expected to fail
      }
      assertThat(promise.future().failed()).isTrue();
    } finally {
      vertx.close();
    }
  }

  private static final class TestVerticle extends AbstractVerticle {
    private final boolean shouldSucceed;

    TestVerticle(boolean shouldSucceed) {
      this.shouldSucceed = shouldSucceed;
    }

    @Override
    public void start(Promise<Void> startPromise) {
      if (shouldSucceed) {
        startPromise.complete();
      } else {
        startPromise.fail("Simulated failure");
      }
    }
  }
}
