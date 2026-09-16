package com.larpconnect.njall.server.cli;

import com.google.common.collect.ImmutableList;
import java.util.List;

/** Encapsulates CLI options for the migrate subcommand. */
public record MigrationOptions(
    String jdbcUrl,
    String username,
    String password,
    ImmutableList<String> schemas,
    String defaultSchema,
    String serverName,
    String primaryDomain,
    String adminContact) {

  public MigrationOptions(
      String jdbcUrl,
      String username,
      String password,
      List<String> schemas,
      String defaultSchema,
      String serverName,
      String primaryDomain,
      String adminContact) {
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
