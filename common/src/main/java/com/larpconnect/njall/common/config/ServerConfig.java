package com.larpconnect.njall.common.config;

import com.typesafe.config.Config;

/**
 * Server network and metadata configuration.
 *
 * @param host Bind interface address or hostname.
 * @param port Port number to bind to (0 for dynamic ephemeral port).
 * @param name Server identification name.
 * @param primaryDomain Server primary domain.
 * @param adminContact Administrative contact.
 */
public record ServerConfig(
    String host, int port, String name, String primaryDomain, String adminContact) {

  private static final int MIN_PORT = 0;
  private static final int MAX_PORT = 65535;

  // Default server metadata constants matching reference.conf defaults, used as fallbacks for
  // of(host, port) programmatic construction and partial Typesafe configuration trees.
  private static final String DEFAULT_NAME = "default-server";
  private static final String DEFAULT_PRIMARY_DOMAIN = "larpconnect.org";
  private static final String DEFAULT_ADMIN_CONTACT = "admin@larpconnect.org";

  public ServerConfig {
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException("Port must be between 0 and 65535, got: " + port);
    }
  }

  /**
   * Pure factory method for creating a {@link ServerConfig} with default metadata.
   *
   * @param host Bind address.
   * @param port Bind port.
   * @return A new {@link ServerConfig} instance.
   */
  public static ServerConfig of(String host, int port) {
    return of(host, port, DEFAULT_NAME, DEFAULT_PRIMARY_DOMAIN, DEFAULT_ADMIN_CONTACT);
  }

  /**
   * Pure factory method for creating a {@link ServerConfig} with full metadata.
   *
   * @param host Bind address.
   * @param port Bind port.
   * @param name Server identification name.
   * @param primaryDomain Server primary domain.
   * @param adminContact Administrative contact.
   * @return A new {@link ServerConfig} instance.
   */
  public static ServerConfig of(
      String host, int port, String name, String primaryDomain, String adminContact) {
    return new ServerConfig(host, port, name, primaryDomain, adminContact);
  }

  /**
   * Extracts {@link ServerConfig} from a Typesafe {@link Config} instance.
   *
   * @param config The root or scoped Typesafe configuration.
   * @return The parsed {@link ServerConfig}.
   */
  public static ServerConfig fromConfig(Config config) {
    var host = config.getString("larpconnect.server.host");
    var port = config.getInt("larpconnect.server.port");
    var name =
        config.hasPath("larpconnect.server.name")
            ? config.getString("larpconnect.server.name")
            : DEFAULT_NAME;
    var primaryDomain =
        config.hasPath("larpconnect.server.primary-domain")
            ? config.getString("larpconnect.server.primary-domain")
            : DEFAULT_PRIMARY_DOMAIN;
    var adminContact =
        config.hasPath("larpconnect.server.admin-contact")
            ? config.getString("larpconnect.server.admin-contact")
            : DEFAULT_ADMIN_CONTACT;
    return of(host, port, name, primaryDomain, adminContact);
  }
}
