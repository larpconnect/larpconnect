package org.larpconnect.data.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.context.TenantContext;

public final class HibernateServiceTest {

  @Test
  public void testCurrentTenantIdentifierResolver() {
    CurrentTenantIdentifierResolverImpl resolver = new CurrentTenantIdentifierResolverImpl();
    assertThat(resolver.validateExistingCurrentSessions()).isTrue();

    TenantContext.clear();
    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("njall_core_default");

    TenantContext.setTenantSchema("njall_test_schema");
    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("njall_test_schema");
    TenantContext.clear();
  }

  @Test
  public void testMultiTenantConnectionProvider() throws SQLException {
    ConnectionProvider mockProvider = mock(ConnectionProvider.class);
    Connection mockConn = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    org.larpconnect.data.schema.StudioRoutingService mockRouting =
        mock(org.larpconnect.data.schema.StudioRoutingService.class);

    when(mockProvider.getConnection()).thenReturn(mockConn);
    when(mockConn.createStatement()).thenReturn(mockStatement);
    when(mockRouting.getSchemaName("njall_tenant"))
        .thenReturn(java.util.Optional.of("njall_tenant"));
    when(mockRouting.getSchemaName("njall_error")).thenReturn(java.util.Optional.of("njall_error"));

    DefaultMultiTenantConnectionProvider provider =
        new DefaultMultiTenantConnectionProvider(mockProvider, mockRouting);

    // getAnyConnection / releaseAnyConnection
    assertThat(provider.getAnyConnection()).isEqualTo(mockConn);
    provider.releaseAnyConnection(mockConn);
    verify(mockProvider, times(1)).closeConnection(mockConn);

    // getConnection
    Connection resultConn = provider.getConnection("njall_tenant");
    assertThat(resultConn).isEqualTo(mockConn);
    verify(mockStatement, times(1)).execute("SET SCHEMA 'njall_tenant'");

    // getConnection exception handling
    doThrow(new SQLException("Mock DB error"))
        .when(mockStatement)
        .execute("SET SCHEMA 'njall_error'");
    assertThatThrownBy(() -> provider.getConnection("njall_error"))
        .isInstanceOf(SQLException.class);
    verify(mockProvider, times(2)).closeConnection(mockConn);

    // releaseConnection
    provider.releaseConnection("njall_tenant", mockConn);
    verify(mockProvider, times(3)).closeConnection(mockConn);
  }

  @Test
  public void testMultiTenantConnectionProviderUnwrap() {
    ConnectionProvider mockProvider = mock(ConnectionProvider.class);
    org.larpconnect.data.schema.StudioRoutingService mockRouting =
        mock(org.larpconnect.data.schema.StudioRoutingService.class);
    DefaultMultiTenantConnectionProvider provider =
        new DefaultMultiTenantConnectionProvider(mockProvider, mockRouting);

    // supportsAggressiveRelease
    assertThat(provider.supportsAggressiveRelease()).isFalse();

    // isUnwrappableAs
    assertThat(provider.isUnwrappableAs(MultiTenantConnectionProvider.class)).isTrue();
    when(mockProvider.isUnwrappableAs(ConnectionProvider.class)).thenReturn(true);
    assertThat(provider.isUnwrappableAs(ConnectionProvider.class)).isTrue();
    when(mockProvider.isUnwrappableAs(String.class)).thenReturn(false);
    assertThat(provider.isUnwrappableAs(String.class)).isFalse();

    // unwrap
    assertThat(provider.unwrap(MultiTenantConnectionProvider.class)).isEqualTo(provider);
    when(mockProvider.unwrap(ConnectionProvider.class)).thenReturn(mockProvider);
    assertThat(provider.unwrap(ConnectionProvider.class)).isEqualTo(mockProvider);
  }

  @Test
  public void testDefaultHibernateService() throws Exception {
    org.larpconnect.data.schema.StudioRoutingService mockRouting =
        mock(org.larpconnect.data.schema.StudioRoutingService.class);
    DefaultHibernateService service = new DefaultHibernateService(mockRouting);
    assertThat(service.getSessionFactory()).isNull();
    service.shutDown();
  }

  @Test
  public void testDefaultHibernateServiceLifecycle() throws Exception {
    Connection mockConn = mock(Connection.class);
    DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
    Statement mockStatement = mock(Statement.class);
    ResultSet mockResultSet = mock(ResultSet.class);

    when(mockConn.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    when(mockMetaData.getDatabaseProductVersion()).thenReturn("17.0.0");
    when(mockMetaData.getDatabaseMajorVersion()).thenReturn(17);
    when(mockMetaData.getDatabaseMinorVersion()).thenReturn(0);
    when(mockMetaData.getConnection()).thenReturn(mockConn);
    when(mockMetaData.getTypeInfo()).thenReturn(mockResultSet);
    when(mockMetaData.getIdentifierQuoteString()).thenReturn("\"");
    when(mockMetaData.getExtraNameCharacters()).thenReturn("");
    when(mockConn.createStatement()).thenReturn(mockStatement);

    org.hibernate.engine.jdbc.connections.spi.ConnectionProvider mockProvider =
        mock(org.hibernate.engine.jdbc.connections.spi.ConnectionProvider.class);
    when(mockProvider.getConnection()).thenReturn(mockConn);

    org.larpconnect.data.schema.StudioRoutingService mockRouting =
        mock(org.larpconnect.data.schema.StudioRoutingService.class);
    DefaultHibernateService service =
        new DefaultHibernateService(mockRouting) {
          @Override
          org.hibernate.engine.jdbc.connections.spi.ConnectionProvider createConnectionProvider(
              java.util.Map<String, Object> props) {
            return mockProvider;
          }
        };

    service.startUp();
    assertThat(service.getSessionFactory()).isNotNull();

    service.shutDown();
    assertThat(service.getSessionFactory().isClosed()).isTrue();
  }

  @Test
  public void testHibernateModule() {
    HibernateModule module = new HibernateModule();
    SessionFactory mockFactory = mock(SessionFactory.class);
    HibernateService dummyService = new DummyHibernateService(mockFactory);

    assertThat(module.provideSessionFactory(dummyService)).isEqualTo(mockFactory);
  }

  private static final class DummyHibernateService
      extends com.google.common.util.concurrent.AbstractIdleService implements HibernateService {
    private final SessionFactory sessionFactory;

    DummyHibernateService(SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
    }

    @Override
    public SessionFactory getSessionFactory() {
      return sessionFactory;
    }

    @Override
    protected void startUp() {}

    @Override
    protected void shutDown() {}
  }
}
