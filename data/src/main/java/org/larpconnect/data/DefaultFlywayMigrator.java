package org.larpconnect.data;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.flywaydb.core.Flyway;

/** Default implementation of {@link FlywayMigrator} using Flyway. */
@ThreadSafe
final class DefaultFlywayMigrator implements FlywayMigrator {
  private final Provider<Flyway> flywayProvider;

  @Inject
  DefaultFlywayMigrator(Provider<Flyway> flywayProvider) {
    this.flywayProvider = flywayProvider;
  }

  @Override
  public void migrate(DatabaseConfiguration config) {
    flywayProvider.get().migrate();
  }
}
