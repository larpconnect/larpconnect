package org.larpconnect.data.context;

import java.util.function.Supplier;

/** Context for managing and resolving the active tenant identifier. */
public final class TenantContext {
  private static final ThreadLocal<String> THREAD_LOCAL_TENANT = new ThreadLocal<>();
  private static Supplier<String> tenantSupplier = THREAD_LOCAL_TENANT::get;

  private TenantContext() {}

  /** Sets a custom supplier for resolving the tenant identifier (e.g. Vert.x context-based). */
  public static void setTenantSupplier(Supplier<String> supplier) {
    tenantSupplier = supplier;
  }

  /** Sets the current tenant identifier on the thread-local fallback. */
  public static void setTenantSchema(String tenant) {
    THREAD_LOCAL_TENANT.set(tenant);
  }

  /** Gets the current tenant identifier using the active supplier. */
  public static String getTenantSchema() {
    return tenantSupplier.get();
  }

  /** Clears the thread-local tenant identifier. */
  public static void clear() {
    THREAD_LOCAL_TENANT.remove();
  }
}
