package com.larpconnect.njall.server;

import java.util.Arrays;

/**
 * Command-line argument parser for Project Njall server.
 *
 * @param migrate Whether the --migrate flag is present.
 */
record CliArgs(boolean migrate) {

  static final String MIGRATE_FLAG = "--migrate";

  static CliArgs parse(String[] args) {
    if (args == null || args.length == 0) {
      return of(false);
    }
    var shouldMigrate = Arrays.asList(args).contains(MIGRATE_FLAG);
    return of(shouldMigrate);
  }

  static CliArgs of(boolean migrate) {
    return new CliArgs(migrate);
  }
}
