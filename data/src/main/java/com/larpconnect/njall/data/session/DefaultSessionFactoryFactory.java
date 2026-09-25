package com.larpconnect.njall.data.session;

import com.google.inject.Inject;
import com.larpconnect.njall.data.config.SessionConfig;
import java.util.Collection;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;

final class DefaultSessionFactoryFactory implements SessionFactoryFactory {

  @Inject
  DefaultSessionFactoryFactory() {}

  @Override
  public SessionFactory create(SessionConfig config, Collection<Class<?>> annotatedClasses) {
    var registry = buildRegistry(config);
    return buildSessionFactory(registry, annotatedClasses);
  }

  private StandardServiceRegistry buildRegistry(SessionConfig config) {
    var builder =
        new StandardServiceRegistryBuilder()
            .applySetting(AvailableSettings.JAKARTA_JDBC_URL, config.jdbcUrl())
            .applySetting(AvailableSettings.JAKARTA_JDBC_USER, config.username())
            .applySetting(AvailableSettings.POOL_SIZE, String.valueOf(config.maxPoolSize()))
            .applySetting(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName())
            .applySetting(AvailableSettings.ALLOW_METADATA_ON_BOOT, "false")
            .applySetting(AvailableSettings.SHOW_SQL, "false")
            .applySetting(AvailableSettings.FORMAT_SQL, "false")
            .applySetting(AvailableSettings.HIGHLIGHT_SQL, "false");

    if (config.hasPassword()) {
      builder.applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, config.password());
    }

    return builder.build();
  }

  private SessionFactory buildSessionFactory(
      StandardServiceRegistry registry, Collection<Class<?>> annotatedClasses) {
    var metadataSources = new MetadataSources(registry);
    for (var clazz : annotatedClasses) {
      metadataSources.addAnnotatedClass(clazz);
    }
    return metadataSources.getMetadataBuilder().build().getSessionFactoryBuilder().build();
  }
}
