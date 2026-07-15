package org.larpconnect.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;

/** Exposes bindings for the database layer (Hibernate/PostgreSQL). */
public final class DataModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DatabaseConfiguration.class)
        .toProvider(DatabaseConfigurationProvider.class)
        .in(Singleton.class);

    bind(DatabaseMigrator.class).to(DefaultDatabaseMigrator.class).in(Singleton.class);
    bind(FlywayMigrator.class).to(DefaultFlywayMigrator.class).in(Singleton.class);
    bind(HibernateFactory.class).to(DefaultHibernateFactory.class).in(Singleton.class);
    bind(TestTableDao.class).to(DefaultTestTableDao.class).in(Singleton.class);

    // TODO: Separate DAOs from DTOs in separate packages.
    bind(SessionFactory.class).toProvider(SessionFactoryProvider.class).in(Singleton.class);
  }

  @Provides
  Flyway provideFlyway(DatabaseConfiguration config) {
    return Flyway.configure()
        .dataSource(config.getJdbcUrl(), config.username(), config.password())
        .locations("classpath:db/migration")
        .load();
  }
}
