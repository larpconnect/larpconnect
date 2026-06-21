package org.larpconnect.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CommonHelper} and {@link CommonModule}. */
public final class CommonHelperTest {
  @Test
  public void greet_withName_returnsGreeting() {
    CommonHelper helper = new CommonHelper();
    assertThat(helper.greet("LarpConnect")).isEqualTo("Hello, LarpConnect");
  }

  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new CommonModule());
    assertThat(injector).isNotNull();
  }
}
