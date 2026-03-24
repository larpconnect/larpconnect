package com.larpconnect.njall.data.dao;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

final class TenantResolver implements CurrentTenantIdentifierResolver<String> {

  TenantResolver() {}

  @Override
  public String resolveCurrentTenantIdentifier() {
    return "njall";
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
