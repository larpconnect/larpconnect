package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.larpconnect.common.MapEnvironment;

/** Unit tests for {@link DatabaseConfigurationProvider}. */
public final class DatabaseConfigurationProviderTest {
  @Test
  public void get_withDefaultEnvironment_returnsDefaultConfiguration() {
    MapEnvironment environment = new MapEnvironment(Map.of());
    DatabaseConfigurationProvider provider = new DatabaseConfigurationProvider(environment);

    assertThat(provider.get())
        .isEqualTo(new DatabaseConfiguration("localhost", 5432, "larpconnect", "postgres", ""));
  }

  @Test
  public void get_withCustomEnvironment_returnsCustomConfiguration() {
    MapEnvironment environment =
        new MapEnvironment(
            Map.of(
                "DB_HOST", "db-host",
                "DB_PORT", "5433",
                "DB_DATABASE", "custom-db",
                "DB_USERNAME", "custom-user",
                "DB_PASSWORD", "custom-pass"));
    DatabaseConfigurationProvider provider = new DatabaseConfigurationProvider(environment);

    assertThat(provider.get())
        .isEqualTo(
            new DatabaseConfiguration("db-host", 5433, "custom-db", "custom-user", "custom-pass"));
  }

  @Test
  public void get_withInvalidPort_returnsDefaultPort() {
    MapEnvironment environment = new MapEnvironment(Map.of("DB_PORT", "invalid"));
    DatabaseConfigurationProvider provider = new DatabaseConfigurationProvider(environment);

    assertThat(provider.get())
        .isEqualTo(new DatabaseConfiguration("localhost", 5432, "larpconnect", "postgres", ""));
  }

  @Test
  public void get_withBlankPort_returnsDefaultPort() {
    MapEnvironment environment = new MapEnvironment(Map.of("DB_PORT", "   "));
    DatabaseConfigurationProvider provider = new DatabaseConfigurationProvider(environment);

    assertThat(provider.get())
        .isEqualTo(new DatabaseConfiguration("localhost", 5432, "larpconnect", "postgres", ""));
  }
}
