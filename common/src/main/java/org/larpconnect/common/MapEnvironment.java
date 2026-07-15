package org.larpconnect.common;

import java.util.Map;
import java.util.Optional;

/** An {@link Environment} implementation backed by a {@link Map}. */
public final class MapEnvironment implements Environment {
  private final Map<String, String> values;

  public MapEnvironment(Map<String, String> values) {
    this.values = Map.copyOf(values);
  }

  @Override
  public Optional<String> get(String name) {
    return Optional.ofNullable(values.get(name));
  }
}
