package com.larpconnect.njall.server.cli;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Encapsulates CLI options for the migrate subcommand. */
@Immutable
public record MigrationOptions(
    Optional<String> jdbcUrl,
    Optional<String> username,
    Optional<String> password,
    Optional<Boolean> trustAuth,
    Optional<ImmutableList<String>> schemas,
    Optional<String> defaultSchema,
    Optional<String> serverName,
    Optional<String> primaryDomain,
    Optional<String> adminContact) {

  public MigrationOptions(
      @Nullable String jdbcUrl,
      @Nullable String username,
      @Nullable String password,
      @Nullable Boolean trustAuth,
      @Nullable List<String> schemas,
      @Nullable String defaultSchema,
      @Nullable String serverName,
      @Nullable String primaryDomain,
      @Nullable String adminContact) {
    this(
        Optional.ofNullable(jdbcUrl),
        Optional.ofNullable(username),
        Optional.ofNullable(password),
        Optional.ofNullable(trustAuth),
        Optional.ofNullable(schemas).map(ImmutableList::copyOf),
        Optional.ofNullable(defaultSchema),
        Optional.ofNullable(serverName),
        Optional.ofNullable(primaryDomain),
        Optional.ofNullable(adminContact));
  }

  public MigrationOptions(
      @Nullable String jdbcUrl,
      @Nullable String username,
      @Nullable String password,
      @Nullable List<String> schemas,
      @Nullable String defaultSchema,
      @Nullable String serverName,
      @Nullable String primaryDomain,
      @Nullable String adminContact) {
    this(
        jdbcUrl,
        username,
        password,
        null,
        schemas,
        defaultSchema,
        serverName,
        primaryDomain,
        adminContact);
  }
}
