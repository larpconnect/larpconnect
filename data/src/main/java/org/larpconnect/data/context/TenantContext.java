package org.larpconnect.data.context;

import java.util.function.Supplier;

/** Context for managing and resolving the active tenant identifier. */
public final class TenantContext {
  private static Supplier<String> tenantSupplier = () -> "njall_core_default";

  private TenantContext() {}

  /** Sets a custom supplier for resolving the tenant identifier (e.g. Vert.x context-based). */
  public static void setTenantSupplier(Supplier<String> supplier) {
    tenantSupplier = supplier;
  }

  /** Gets the current tenant identifier using the active supplier. */
  public static String getTenantSchema() {
    return tenantSupplier.get();
  }
}
