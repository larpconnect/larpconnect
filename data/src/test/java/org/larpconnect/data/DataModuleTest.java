package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for the database layer configuration. */
public final class DataModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new DataModule());
    assertThat(injector).isNotNull();
  }
}
