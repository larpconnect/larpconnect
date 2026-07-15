package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the database layer configuration. */
@ExtendWith(MockitoExtension.class)
public final class DataModuleTest {
  @Mock private DatabaseConfiguration config;
  @Mock private HibernateFactory hibernateFactory;
  @Mock private StandardServiceRegistryBuilder registryBuilder;
  @Mock private StandardServiceRegistry registry;
  @Mock private MetadataSources metadataSources;
  @Mock private MetadataBuilder metadataBuilder;
  @Mock private Metadata metadata;
  @Mock private SessionFactoryBuilder sessionFactoryBuilder;
  @Mock private SessionFactory sessionFactoryMock;
  @Mock private Flyway flyway;

  @Inject private DatabaseMigrator databaseMigrator;
  @Inject private FlywayMigrator flywayMigrator;
  @Inject private HibernateFactory factory;
  @Inject private TestTableDao testTableDao;
  @Inject private Provider<SessionFactory> sessionFactoryProvider;

  @Test
  public void createInjector_withModule_resolvesBindings() {
    Guice.createInjector(new DataModule()).injectMembers(this);

    assertThat(databaseMigrator).isNotNull();
    assertThat(flywayMigrator).isNotNull();
    assertThat(factory).isNotNull();
    assertThat(testTableDao).isNotNull();
    assertThat(sessionFactoryProvider).isNotNull();
  }

  @Test
  public void provideSessionFactory_configuresSettingsAndAnnotatedClasses() {
    when(config.getJdbcUrl()).thenReturn("jdbc:postgresql://localhost:5432/db");
    when(config.username()).thenReturn("user");
    when(config.password()).thenReturn("pass");

    when(hibernateFactory.createRegistryBuilder()).thenReturn(registryBuilder);
    when(registryBuilder.applySettings(anyMap())).thenReturn(registryBuilder);
    when(registryBuilder.build()).thenReturn(registry);
    when(hibernateFactory.createMetadataSources(registry)).thenReturn(metadataSources);
    when(metadataSources.getMetadataBuilder()).thenReturn(metadataBuilder);
    when(metadataBuilder.build()).thenReturn(metadata);
    when(metadata.getSessionFactoryBuilder()).thenReturn(sessionFactoryBuilder);
    when(sessionFactoryBuilder.build()).thenReturn(sessionFactoryMock);

    SessionFactoryProvider provider = new SessionFactoryProvider(config, hibernateFactory);
    SessionFactory sessionFactory = provider.get();

    assertThat(sessionFactory).isSameAs(sessionFactoryMock);
    verify(metadataSources).addAnnotatedClass(TestTable.class);
  }

  @Test
  public void defaultFlywayMigrator_canBeInstantiated() {
    DefaultFlywayMigrator migrator = new DefaultFlywayMigrator(() -> flyway);
    assertThat(migrator).isNotNull();
  }
}
