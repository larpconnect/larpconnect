package com.larpconnect.njall.data.dao;

import io.vertx.sqlclient.Pool;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPool;

final class SchemaSelectionConnectionProvider extends DefaultSqlClientPool {

  private static final long serialVersionUID = 1L;
  private static final Pattern VALID_TENANT_PATTERN = Pattern.compile("^[a-z0-9]+$");

  SchemaSelectionConnectionProvider() {}

  @Override
  public Pool getTenantPool(String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant ID cannot be null or empty");
    }
    if (!VALID_TENANT_PATTERN.matcher(tenantId).matches()) {
      throw new IllegalArgumentException("Invalid tenant ID: " + tenantId);
    }
    return getPool();
  }

  @Override
  public CompletionStage<ReactiveConnection> getConnection(String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant ID cannot be null or empty");
    }
    if (!VALID_TENANT_PATTERN.matcher(tenantId).matches()) {
      throw new IllegalArgumentException("Invalid tenant ID: " + tenantId);
    }

    return super.getConnection(tenantId)
        .thenCompose(
            connection ->
                connection.execute("SET search_path TO " + tenantId).thenApply(v -> connection));
  }
}
