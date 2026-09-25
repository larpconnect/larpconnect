package com.larpconnect.njall.data.config;

/** Factory for constructing path-scoped {@link SessionConfig} instances. */
public interface SessionConfigFactory {

  /**
   * Constructs a {@link SessionConfig} populated from the configured path in Typesafe Config.
   *
   * @param path The configuration path (e.g. {@code "larpconnect.data.database.admin"}).
   * @return A new {@link SessionConfig} instance.
   */
  SessionConfig create(String path);
}
