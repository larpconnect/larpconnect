package org.larpconnect.common;

import java.util.Optional;

/** Abstraction for reading configuration properties from the environment. */
public interface Environment {
  /**
   * Returns the environment variable value for the given name.
   *
   * @param name The name of the environment variable.
   * @return Optional containing the value, or empty if not present.
   */
  Optional<String> get(String name);

  /**
   * Returns the environment variable value, or the default value if not present.
   *
   * @param name The name of the environment variable.
   * @param defaultValue The default value.
   * @return The environment variable value or default value.
   */
  default String getOrDefault(String name, String defaultValue) {
    return get(name).orElse(defaultValue);
  }
}
