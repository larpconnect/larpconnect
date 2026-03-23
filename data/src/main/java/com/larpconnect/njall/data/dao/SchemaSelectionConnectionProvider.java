package com.larpconnect.njall.data.dao;

import io.vertx.sqlclient.Pool;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPool;

final class SchemaSelectionConnectionProvider extends DefaultSqlClientPool {

  private static final long serialVersionUID = 1L;

  SchemaSelectionConnectionProvider() {}

  @Override
  public Pool getTenantPool(String tenantId) {
    if (tenantId == null || tenantId.isEmpty() || tenantId.equals("public")) {
      return getPool();
    }
    return getPool();
  }
}
