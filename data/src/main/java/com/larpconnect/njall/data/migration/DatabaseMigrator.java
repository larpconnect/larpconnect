package com.larpconnect.njall.data.migration;

/** Contract for executing database migrations. */
public interface DatabaseMigrator {

  /**
   * Executes pending database migrations.
   *
   * @return The number of migrations successfully applied.
   */
  int migrate();
}
