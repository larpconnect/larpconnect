package com.larpconnect.njall.server.cli;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Encapsulates CLI options for the migrate subcommand. */
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

  public MigrationOptions {
    requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
    requireNonNull(username, "username cannot be null");
    requireNonNull(password, "password cannot be null");
    requireNonNull(trustAuth, "trustAuth cannot be null");
    requireNonNull(schemas, "schemas cannot be null");
    requireNonNull(defaultSchema, "defaultSchema cannot be null");
    requireNonNull(serverName, "serverName cannot be null");
    requireNonNull(primaryDomain, "primaryDomain cannot be null");
    requireNonNull(adminContact, "adminContact cannot be null");
  }

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
