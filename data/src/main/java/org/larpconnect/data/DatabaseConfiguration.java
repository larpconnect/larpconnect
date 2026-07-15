package org.larpconnect.data;

/** Represents the database connection configuration. */
public record DatabaseConfiguration(
    String host, int port, String database, String username, String password) {

  public static DatabaseConfiguration fromEnv() {
    return create(
        System.getenv().getOrDefault("DB_HOST", "localhost"),
        parsePort(System.getenv().get("DB_PORT")),
        System.getenv().getOrDefault("DB_DATABASE", "larpconnect"),
        System.getenv().getOrDefault("DB_USERNAME", "postgres"),
        System.getenv().getOrDefault("DB_PASSWORD", "postgres"));
  }

  static int parsePort(String portStr) {
    if (portStr == null || portStr.isBlank()) {
      return 5432;
    }
    try {
      return Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      return 5432;
    }
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
    return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
  }
}
