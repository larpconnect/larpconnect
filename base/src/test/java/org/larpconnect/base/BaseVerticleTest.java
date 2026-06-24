package org.larpconnect.base;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Promise;
import org.junit.jupiter.api.Test;

/** Unit tests for BaseVerticle and BaseVerticleProvider. */
public final class BaseVerticleTest {
  @Test
  public void start_whenInvoked_completesPromise() {
    BaseVerticle verticle = new BaseVerticle();
    Promise<Void> promise = Promise.promise();
    verticle.start(promise);
    assertThat(promise.future().succeeded()).isTrue();
  }

  @Test
  public void getVerticle_whenCalled_returnsInstance() {
    BaseVerticle verticle = new BaseVerticle();
    BaseVerticleProvider provider = new BaseVerticleProvider(verticle);
    assertThat(provider.getVerticle()).isSameAs(verticle);
  }
}
