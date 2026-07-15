package org.larpconnect.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.larpconnect.common.Environment;

/** Guice provider that compiles database configurations from the injected {@link Environment}. */
public final class DatabaseConfigurationProvider implements Provider<DatabaseConfiguration> {
  private final Environment environment;

  @Inject
  DatabaseConfigurationProvider(Environment environment) {
    this.environment = environment;
  }

  @Override
  public DatabaseConfiguration get() {
    // Integration function: only delegates to other class functions
    return create(getHost(), getPort(), getDatabase(), getUsername(), getPassword());
  }

  private String getHost() {
    return environment.getOrDefault("DB_HOST", "localhost");
  }

  private int getPort() {
    String rawPort = environment.get("DB_PORT").orElse(null);
    if (rawPort == null || rawPort.isBlank()) {
      return 5432;
    }
    try {
      return Integer.parseInt(rawPort);
    } catch (NumberFormatException e) {
      return 5432;
    }
  }

  private String getDatabase() {
    return environment.getOrDefault("DB_DATABASE", "larpconnect");
  }

  private String getUsername() {
    return environment.getOrDefault("DB_USERNAME", "postgres");
  }

  private String getPassword() {
    return environment.getOrDefault("DB_PASSWORD", "");
  }

  private DatabaseConfiguration create(
      String host, int port, String database, String username, String password) {
    return new DatabaseConfiguration(host, port, database, username, password);
  }
}
