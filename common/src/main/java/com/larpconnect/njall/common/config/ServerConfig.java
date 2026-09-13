package com.larpconnect.njall.common.config;

import static java.util.Objects.requireNonNull;

import com.typesafe.config.Config;

/**
 * Server network configuration.
 *
 * @param host Bind interface address or hostname.
 * @param port Port number to bind to (0 for dynamic ephemeral port).
 */
public record ServerConfig(String host, int port) {

  private static final int MIN_PORT = 0;
  private static final int MAX_PORT = 65535;

  public ServerConfig {
    requireNonNull(host, "host cannot be null");
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException("Port must be between 0 and 65535, got: " + port);
    }
  }

  /**
   * Pure factory method for creating a {@link ServerConfig}.
   *
   * @param host Bind address.
   * @param port Bind port.
   * @return A new {@link ServerConfig} instance.
   */
  public static ServerConfig of(String host, int port) {
    return new ServerConfig(host, port);
  }

  /**
   * Extracts {@link ServerConfig} from a Typesafe {@link Config} instance.
   *
   * @param config The root or scoped Typesafe configuration.
   * @return The parsed {@link ServerConfig}.
   */
  public static ServerConfig fromConfig(Config config) {
    requireNonNull(config, "config cannot be null");
    var host = config.getString("larpconnect.server.host");
    var port = config.getInt("larpconnect.server.port");
    return of(host, port);
  }
}
