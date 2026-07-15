package org.larpconnect.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Environment}, {@link SystemEnvironment}, and {@link MapEnvironment}. */
public final class EnvironmentTest {
  @Test
  public void mapEnvironment_get_returnsCorrectValue() {
    MapEnvironment environment = new MapEnvironment(Map.of("KEY", "VALUE"));

    assertThat(environment.get("KEY")).hasValue("VALUE");
    assertThat(environment.get("MISSING")).isEmpty();
  }

  @Test
  public void environment_getOrDefault_returnsCorrectValue() {
    Environment environment = new MapEnvironment(Map.of("KEY", "VALUE"));

    assertThat(environment.getOrDefault("KEY", "DEFAULT")).isEqualTo("VALUE");
    assertThat(environment.getOrDefault("MISSING", "DEFAULT")).isEqualTo("DEFAULT");
  }

  @Test
  public void systemEnvironment_get_works() {
    SystemEnvironment environment = new SystemEnvironment();

    // Since System.getenv() is system-dependent, we can test with a known standard env variable
    // or just check that it doesn't crash and returns empty for a random key.
    assertThat(environment.get("NON_EXISTENT_KEY_XYZ_123")).isEmpty();

    // If PATH exists, we verify it is returned
    String path = System.getenv("PATH");
    if (path != null) {
      assertThat(environment.get("PATH")).hasValue(path);
    }
  }
}
