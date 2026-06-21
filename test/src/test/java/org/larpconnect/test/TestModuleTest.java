package org.larpconnect.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for the shared test framework skeleton. */
public final class TestModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new TestModule());
    assertThat(injector).isNotNull();
  }
}
