package com.larpconnect.njall.data.dao;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

final class TenantResolver implements CurrentTenantIdentifierResolver<String> {

  TenantResolver() {}

  @Override
  public String resolveCurrentTenantIdentifier() {
    String currentTenant = TenantContext.getCurrentTenant();
    return currentTenant == null ? "njall_base" : currentTenant;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
