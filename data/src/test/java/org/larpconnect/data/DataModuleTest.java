package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/** Unit tests for the database layer configuration. */
public final class DataModuleTest {
  @Test
  public void createInjector_withModule_isNotNull() {
    Injector injector = Guice.createInjector(new DataModule());
    assertThat(injector).isNotNull();
  }

  @Test
  public void provideSessionFactory_executesSuccessfullyWithMocks() {
    DatabaseConfiguration config = mock(DatabaseConfiguration.class);
    when(config.getJdbcUrl()).thenReturn("jdbc:postgresql://localhost:5432/db");
    when(config.username()).thenReturn("user");
    when(config.password()).thenReturn("pass");

    StandardServiceRegistry mockRegistry = mock(StandardServiceRegistry.class);
    MetadataBuilder mockMetadataBuilder = mock(MetadataBuilder.class);
    Metadata mockMetadata = mock(Metadata.class);
    SessionFactoryBuilder mockSfBuilder = mock(SessionFactoryBuilder.class);
    SessionFactory mockSf = mock(SessionFactory.class);

    try (MockedConstruction<StandardServiceRegistryBuilder> mockedBuilder =
            Mockito.mockConstruction(
                StandardServiceRegistryBuilder.class,
                (mock, context) -> {
                  when(mock.applySettings(Mockito.anyMap())).thenReturn(mock);
                  when(mock.build()).thenReturn(mockRegistry);
                });
        MockedConstruction<MetadataSources> mockedSources =
            Mockito.mockConstruction(
                MetadataSources.class,
                (mock, context) -> {
                  when(mock.getMetadataBuilder()).thenReturn(mockMetadataBuilder);
                })) {

      when(mockMetadataBuilder.build()).thenReturn(mockMetadata);
      when(mockMetadata.getSessionFactoryBuilder()).thenReturn(mockSfBuilder);
      when(mockSfBuilder.build()).thenReturn(mockSf);

      SessionFactoryProvider provider = new SessionFactoryProvider(config);
      SessionFactory sessionFactory = provider.get();

      assertThat(sessionFactory).isSameAs(mockSf);
      assertThat(mockedBuilder).isNotNull();
      assertThat(mockedSources.constructed()).hasSize(1);
      verify(mockedSources.constructed().get(0)).addAnnotatedClass(TestTableEntity.class);
    }
  }

  @Test
  public void defaultFlywayMigrator_canBeInstantiated() {
    DefaultFlywayMigrator migrator = new DefaultFlywayMigrator();
    assertThat(migrator).isNotNull();
  }
}
