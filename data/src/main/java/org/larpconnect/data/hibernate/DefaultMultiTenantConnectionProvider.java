package org.larpconnect.data.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.larpconnect.data.schema.StudioRoutingService;

/**
 * Default multi-tenant connection provider that switches PostgreSQL schemas using {@code SET
 * SCHEMA}.
 */
public final class DefaultMultiTenantConnectionProvider
    extends AbstractMultiTenantConnectionProvider<String> {
  private static final long serialVersionUID = 1L;

  private final transient ConnectionProvider connectionProvider;
  private final transient StudioRoutingService routingService;

  public DefaultMultiTenantConnectionProvider(
      ConnectionProvider connectionProvider, StudioRoutingService routingService) {
    this.connectionProvider = connectionProvider;
    this.routingService = routingService;
  }

  @Override
  protected ConnectionProvider getAnyConnectionProvider() {
    return connectionProvider;
  }

  @Override
  protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
    return connectionProvider;
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    Connection connection = super.getConnection(tenantIdentifier);
    String schema = routingService.getSchemaName(tenantIdentifier).orElse(tenantIdentifier);
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET SCHEMA '" + schema + "'");
    } catch (SQLException e) {
      super.releaseConnection(tenantIdentifier, connection);
      throw e;
    }
    return connection;
  }
}
