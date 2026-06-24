package org.larpconnect.data.hibernate;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.larpconnect.data.context.TenantContext;

/** Resolves the current tenant identifier from {@link TenantContext}. */
public final class CurrentTenantIdentifierResolverImpl
    implements CurrentTenantIdentifierResolver<String> {
  @Override
  public String resolveCurrentTenantIdentifier() {
    String tenantSchema = TenantContext.getTenantSchema();
    return tenantSchema != null ? tenantSchema : "njall_core_default";
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
