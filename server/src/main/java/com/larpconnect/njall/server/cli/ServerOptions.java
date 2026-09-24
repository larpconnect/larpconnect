package com.larpconnect.njall.server.cli;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Encapsulates CLI options for the server subcommand. */
public record ServerOptions(
    Optional<String> host,
    Optional<Integer> port,
    Optional<String> name,
    Optional<String> primaryDomain,
    Optional<String> adminContact) {

  public ServerOptions {
    requireNonNull(host, "host cannot be null");
    requireNonNull(port, "port cannot be null");
    requireNonNull(name, "name cannot be null");
    requireNonNull(primaryDomain, "primaryDomain cannot be null");
    requireNonNull(adminContact, "adminContact cannot be null");
  }

  public ServerOptions(
      @Nullable String host,
      @Nullable Integer port,
      @Nullable String name,
      @Nullable String primaryDomain,
      @Nullable String adminContact) {
    this(
        Optional.ofNullable(host),
        Optional.ofNullable(port),
        Optional.ofNullable(name),
        Optional.ofNullable(primaryDomain),
        Optional.ofNullable(adminContact));
  }
}
