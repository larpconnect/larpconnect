package org.larpconnect.queue;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for AMQP queue infrastructure. */
public final class QueueModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new QueueModule());
    assertThat(injector).isNotNull();
  }
}
