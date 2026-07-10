package org.larpconnect.data;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.hibernate.SessionFactory;

/** Exposes bindings for the database layer (Hibernate/PostgreSQL). */
public final class DataModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DatabaseConfiguration.class).toInstance(DatabaseConfiguration.fromEnv());

    bind(DatabaseInitializer.class).to(DatabaseInitializerService.class);
    bind(DatabaseInitializerService.class)
        .to(DefaultDatabaseInitializerService.class)
        .in(Singleton.class);

    bind(FlywayMigrator.class).to(DefaultFlywayMigrator.class).in(Singleton.class);

    bind(TestTableDao.class).to(DefaultTestTableDao.class).in(Singleton.class);

    bind(SessionFactory.class).toProvider(SessionFactoryProvider.class).in(Singleton.class);
  }
}
