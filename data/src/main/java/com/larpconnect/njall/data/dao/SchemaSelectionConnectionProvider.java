package com.larpconnect.njall.data.dao;

import io.vertx.sqlclient.Pool;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPool;

final class SchemaSelectionConnectionProvider extends DefaultSqlClientPool {

  private static final long serialVersionUID = 1L;
  private static final Pattern VALID_SERVER_TENANT_PATTERN =
      Pattern.compile("^njall_server_([a-z]+[a-z0-9]+){3,32}$");

  SchemaSelectionConnectionProvider() {}

  @Override
  public Pool getTenantPool(String tenantId) {
    validateTenantId(tenantId);
    return super.getTenantPool(tenantId);
  }

  @Override
  public CompletionStage<ReactiveConnection> getConnection(String tenantId) {
    validateTenantId(tenantId);

    return super.getConnection()
        .thenCompose(
            connection ->
                connection.execute("SET search_path TO " + tenantId).thenApply(v -> connection));
  }

  private void validateTenantId(String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant ID cannot be null or empty");
    }
    if (tenantId.equals("njall_base")
        || tenantId.equals("njall_admin")
        || tenantId.equals("njall_analytics")) {
      return;
    }
    if (!VALID_SERVER_TENANT_PATTERN.matcher(tenantId).matches()) {
      throw new IllegalArgumentException("Invalid tenant ID: " + tenantId);
    }
  }
}
