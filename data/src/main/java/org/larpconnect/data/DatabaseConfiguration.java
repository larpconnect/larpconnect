package org.larpconnect.data;

/** Represents the database connection configuration. */
public record DatabaseConfiguration(
    String host, int port, String database, String username, String password) {

  /**
   * Factory method to load the configuration from environment variables with defaults.
   *
   * @return A DatabaseConfiguration instance.
   */
  public static DatabaseConfiguration fromEnv() {
    return create(
        System.getenv().getOrDefault("DB_HOST", "localhost"),
        Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "5432")),
        System.getenv().getOrDefault("DB_DATABASE", "larpconnect"),
        System.getenv().getOrDefault("DB_USERNAME", "postgres"),
        System.getenv().getOrDefault("DB_PASSWORD", "postgres"));
  }

  /** Helper factory that isolates the `new` keyword to satisfy Pure Factory Isolation. */
  private static DatabaseConfiguration create(
      String host, int port, String database, String username, String password) {
    return new DatabaseConfiguration(host, port, database, username, password);
  }

  /**
   * Returns the JDBC connection URL for PostgreSQL.
   *
   * @return JDBC URL string.
   */
  public String getJdbcUrl() {
    return String.format("jdbc:tc:postgresql://%s:%d/%s", host, port, database);
  }
}
