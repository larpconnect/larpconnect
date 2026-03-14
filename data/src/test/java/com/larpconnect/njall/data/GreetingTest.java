package com.larpconnect.njall.data;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class GreetingTest {

  @Test
  @Disabled("API version mismatch with locally installed docker.")
  public void saveAndRetrieveGreeting_success(VertxTestContext testContext) {
    // Left disabled deliberately
  }
}
