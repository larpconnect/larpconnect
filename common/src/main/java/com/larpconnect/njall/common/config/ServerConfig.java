package com.larpconnect.njall.common.config;

import com.google.errorprone.annotations.Immutable;

/**
 * Server network and metadata configuration.
 *
 * @param host Bind interface address or hostname.
 * @param port Port number to bind to (0 for dynamic ephemeral port).
 * @param name Server identification name.
 * @param primaryDomain Server primary domain.
 * @param adminContact Administrative contact.
 */
@Immutable
public record ServerConfig(
    String host, int port, String name, String primaryDomain, String adminContact) {

  public static final int MIN_PORT = 0;
  public static final int MAX_PORT = 65535;

  // Default server metadata constants matching reference.conf defaults, used as fallbacks for
  // ServerConfig(host, port) programmatic construction and partial Typesafe configuration trees.
  public static final String DEFAULT_NAME = "default-server";
  public static final String DEFAULT_PRIMARY_DOMAIN = "larpconnect.org";
  public static final String DEFAULT_ADMIN_CONTACT = "admin@larpconnect.org";

  public ServerConfig {
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException("Port must be between 0 and 65535, got: " + port);
    }
  }

  /**
   * Convenience constructor for creating a {@link ServerConfig} with default metadata.
   *
   * @param host Bind address.
   * @param port Bind port.
   */
  public ServerConfig(String host, int port) {
    this(host, port, DEFAULT_NAME, DEFAULT_PRIMARY_DOMAIN, DEFAULT_ADMIN_CONTACT);
  }
}
