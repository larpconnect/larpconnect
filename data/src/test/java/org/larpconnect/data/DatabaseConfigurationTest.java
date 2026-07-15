package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link DatabaseConfiguration}. */
public final class DatabaseConfigurationTest {
  @Test
  public void constructorAndGetters_workAsExpected() {
    DatabaseConfiguration config =
        new DatabaseConfiguration("test-host", 1234, "test-db", "test-user", "test-pass");

    assertThat(config.host()).isEqualTo("test-host");
    assertThat(config.port()).isEqualTo(1234);
    assertThat(config.database()).isEqualTo("test-db");
    assertThat(config.username()).isEqualTo("test-user");
    assertThat(config.password()).isEqualTo("test-pass");
  }

  @Test
  public void fromEnv_returnsNonNullInstance() {
    DatabaseConfiguration config = DatabaseConfiguration.fromEnv();
    assertThat(config).isNotNull();
    assertThat(config.host()).isNotBlank();
    assertThat(config.database()).isNotBlank();
  }

  @Test
  public void parsePort_nullOrBlank_returnsDefaultPort() {
    assertThat(DatabaseConfiguration.parsePort(null)).isEqualTo(5432);
    assertThat(DatabaseConfiguration.parsePort("")).isEqualTo(5432);
    assertThat(DatabaseConfiguration.parsePort("   ")).isEqualTo(5432);
  }

  @Test
  public void parsePort_invalidNumber_returnsDefaultPort() {
    assertThat(DatabaseConfiguration.parsePort("invalid")).isEqualTo(5432);
    assertThat(DatabaseConfiguration.parsePort("12.34")).isEqualTo(5432);
  }

  @Test
  public void parsePort_validNumber_returnsParsedPort() {
    assertThat(DatabaseConfiguration.parsePort(" 5433 ")).isEqualTo(5433);
    assertThat(DatabaseConfiguration.parsePort("5433")).isEqualTo(5433);
    assertThat(DatabaseConfiguration.parsePort("8080")).isEqualTo(8080);
  }
}
