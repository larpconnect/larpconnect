package org.larpconnect.data;

import com.google.inject.Inject;
import javax.annotation.concurrent.ThreadSafe;
import org.flywaydb.core.Flyway;

/** Default implementation of {@link FlywayMigrator} using Flyway. */
@ThreadSafe
final class DefaultFlywayMigrator implements FlywayMigrator {
  @Inject
  DefaultFlywayMigrator() {}

  @Override
  public void migrate(DatabaseConfiguration config) {
    Flyway flyway =
        Flyway.configure()
            .dataSource(config.getJdbcUrl(), config.username(), config.password())
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();
  }
}
