package org.larpconnect.data.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.larpconnect.data.schema.StudioRoutingService;

/** Default multi-tenant connection provider that switches PostgreSQL schemas. */
public final class DefaultMultiTenantConnectionProvider
    extends AbstractMultiTenantConnectionProvider<String> {
  private static final long serialVersionUID = 1L;
  private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-z_]{6,64}$");

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
    if (!SCHEMA_PATTERN.matcher(schema).matches()) {
      super.releaseConnection(tenantIdentifier, connection);
      throw new SQLException("Invalid schema name pattern: " + schema);
    }
    try {
      connection.setSchema(schema);
    } catch (SQLException e) {
      super.releaseConnection(tenantIdentifier, connection);
      throw e;
    }
    return connection;
  }
}
