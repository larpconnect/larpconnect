package com.larpconnect.njall.test;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Guice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TestModuleTest {

  @Test
  @DisplayName("configure initializes successfully in Guice injector")
  void configure_createsInjector_succeeds() {
    assertThatCode(() -> Guice.createInjector(new TestModule())).doesNotThrowAnyException();
  }
}
