package com.larpconnect.njall.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TestModuleTest {

  @Test
  @DisplayName("configure initializes successfully in Guice injector")
  void configure_createsInjector_succeeds() {
    var injector = Guice.createInjector(new TestModule());

    assertThat(injector).isNotNull();
  }
}
