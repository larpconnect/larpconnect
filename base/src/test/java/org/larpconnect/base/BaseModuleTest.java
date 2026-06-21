package org.larpconnect.base;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for the database-to-Vert.x worker bridge. */
public final class BaseModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new BaseModule());
    assertThat(injector).isNotNull();
  }
}
