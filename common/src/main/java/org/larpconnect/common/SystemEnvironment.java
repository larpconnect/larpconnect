package org.larpconnect.common;

import java.util.Optional;

/** Production implementation of {@link Environment} backing onto {@link System#getenv()}. */
public final class SystemEnvironment implements Environment {
  @Override
  public Optional<String> get(String name) {
    return Optional.ofNullable(System.getenv(name));
  }
}
