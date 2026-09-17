package com.larpconnect.njall.server.cli;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Encapsulates CLI options for the migrate subcommand. */
public record MigrationOptions(
    @Nullable String jdbcUrl,
    @Nullable String username,
    @Nullable String password,
    @Nullable ImmutableList<String> schemas,
    @Nullable String defaultSchema,
    @Nullable String serverName,
    @Nullable String primaryDomain,
    @Nullable String adminContact) {

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
        schemas != null ? ImmutableList.copyOf(schemas) : null,
        defaultSchema,
        serverName,
        primaryDomain,
        adminContact);
  }
}
