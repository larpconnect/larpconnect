package com.larpconnect.njall.data.dao;

import io.vertx.core.Vertx;
import javax.annotation.Nullable;

final class TenantContext {

  private static final String TENANT_KEY = "CURRENT_TENANT";

  private TenantContext() {}

  @Nullable
  public static String getCurrentTenant() {
    var context = Vertx.currentContext();
    if (context != null) {
      return (String) context.get(TENANT_KEY);
    }
    return null;
  }

  public static void setCurrentTenant(String tenantId) {
    var context = Vertx.currentContext();
    if (context != null) {
      context.put(TENANT_KEY, formatTenantId(tenantId));
    }
  }

  public static void clear() {
    var context = Vertx.currentContext();
    if (context != null) {
      context.remove(TENANT_KEY);
    }
  }

  static String formatTenantId(@Nullable String serverId) {
    if (serverId == null || serverId.isEmpty()) {
      return "njall_base";
    }
    if (serverId.equals("njall_base")
        || serverId.equals("njall_admin")
        || serverId.equals("njall_analytics")) {
      return serverId;
    }
    if (serverId.startsWith("njall_server_")) {
      return serverId;
    }
    return "njall_server_" + serverId;
  }
}
