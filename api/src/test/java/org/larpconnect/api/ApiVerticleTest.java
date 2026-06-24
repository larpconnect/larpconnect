package org.larpconnect.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Promise;
import org.junit.jupiter.api.Test;

/** Unit tests for ApiVerticle and ApiVerticleProvider. */
public final class ApiVerticleTest {
  @Test
  public void start_whenInvoked_completesPromise() {
    ApiVerticle verticle = new ApiVerticle();
    Promise<Void> promise = Promise.promise();
    verticle.start(promise);
    assertThat(promise.future().succeeded()).isTrue();
  }

  @Test
  public void getVerticle_whenCalled_returnsInstance() {
    ApiVerticle verticle = new ApiVerticle();
    ApiVerticleProvider provider = new ApiVerticleProvider(verticle);
    assertThat(provider.getVerticle()).isSameAs(verticle);
  }
}
