package org.larpconnect.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

/** Guice Provider for Hibernate {@link SessionFactory}. */
class SessionFactoryProvider implements Provider<SessionFactory> {
  private final DatabaseConfiguration config;

  @Inject
  SessionFactoryProvider(DatabaseConfiguration config) {
    this.config = config;
  }

  @Override
  public SessionFactory get() {
    Map<String, Object> settings =
        Map.of(
            AvailableSettings.JAKARTA_JDBC_DRIVER,
            "org.postgresql.Driver",
            AvailableSettings.JAKARTA_JDBC_URL,
            config.getJdbcUrl(),
            AvailableSettings.JAKARTA_JDBC_USER,
            config.username(),
            AvailableSettings.JAKARTA_JDBC_PASSWORD,
            config.password(),
            AvailableSettings.SHOW_SQL,
            "true",
            AvailableSettings.FORMAT_SQL,
            "true",
            AvailableSettings.DIALECT,
            "org.hibernate.dialect.PostgreSQLDialect");

    StandardServiceRegistry registry =
        new StandardServiceRegistryBuilder().applySettings(settings).build();
    MetadataSources sources = new MetadataSources(registry);
    sources.addAnnotatedClass(TestTableEntity.class);

    Metadata metadata = sources.getMetadataBuilder().build();
    return metadata.getSessionFactoryBuilder().build();
  }
}
