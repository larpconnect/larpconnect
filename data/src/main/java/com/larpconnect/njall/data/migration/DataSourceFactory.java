package com.larpconnect.njall.data.migration;

import com.larpconnect.njall.data.config.MigrationConfig;
import javax.sql.DataSource;

/** Factory for constructing transient DataSource instances for migrations. */
public interface DataSourceFactory {

  /**
   * Creates a {@link DataSource} configured with migration parameters.
   *
   * @param config The migration configuration.
   * @return A new configured {@link DataSource}.
   */
  DataSource create(MigrationConfig config);
}
