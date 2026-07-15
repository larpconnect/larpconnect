package org.larpconnect.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;

/** Guice Provider for Hibernate {@link SessionFactory}. */
class SessionFactoryProvider implements Provider<SessionFactory> {
  private final DatabaseConfiguration config;
  private final HibernateFactory hibernateFactory;

  @Inject
  SessionFactoryProvider(DatabaseConfiguration config, HibernateFactory hibernateFactory) {
    this.config = config;
    this.hibernateFactory = hibernateFactory;
  }

  @Override
  public SessionFactory get() {
    Map<String, Object> settings = new HashMap<>();
    settings.put(
        AvailableSettings.JAKARTA_JDBC_DRIVER, "org.testcontainers.jdbc.ContainerDatabaseDriver");
    settings.put(AvailableSettings.JAKARTA_JDBC_URL, config.getJdbcUrl());
    settings.put(AvailableSettings.JAKARTA_JDBC_USER, config.username());
    if (config.password() != null) {
      settings.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, config.password());
    }
    settings.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");

    StandardServiceRegistry registry =
        hibernateFactory.createRegistryBuilder().applySettings(settings).build();
    MetadataSources sources = hibernateFactory.createMetadataSources(registry);
    sources.addAnnotatedClass(TestTable.class);

    Metadata metadata = sources.getMetadataBuilder().build();
    return metadata.getSessionFactoryBuilder().build();
  }
}
