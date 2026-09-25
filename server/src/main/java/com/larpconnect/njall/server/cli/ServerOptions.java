package com.larpconnect.njall.server.cli;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Encapsulates CLI options for the server subcommand. */
public record ServerOptions(
    Optional<String> host,
    Optional<Integer> port,
    Optional<String> name,
    Optional<String> primaryDomain,
    Optional<String> adminContact) {

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
