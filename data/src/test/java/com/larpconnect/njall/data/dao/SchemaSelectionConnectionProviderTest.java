package com.larpconnect.njall.data.dao;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class SchemaSelectionConnectionProviderTest {

  @Test
  void getConnection_invalidTenant_throwsException() {
    var provider = new SchemaSelectionConnectionProvider();

    assertThrows(IllegalArgumentException.class, () -> provider.getConnection((String) null));
    assertThrows(IllegalArgumentException.class, () -> provider.getConnection(""));
    assertThrows(IllegalArgumentException.class, () -> provider.getConnection("some-tenant"));
    assertThrows(
        IllegalArgumentException.class,
        () -> provider.getConnection("njall_server_a")); // length less than 3
  }

  @Test
  void getTenantPool_invalidTenant_throwsException() {
    var provider = new SchemaSelectionConnectionProvider();

    assertThrows(IllegalArgumentException.class, () -> provider.getTenantPool((String) null));
    assertThrows(IllegalArgumentException.class, () -> provider.getTenantPool(""));
    assertThrows(IllegalArgumentException.class, () -> provider.getTenantPool("some-tenant"));
    assertThrows(
        IllegalArgumentException.class,
        () -> provider.getTenantPool("njall_server_a")); // length less than 3
  }

  @Test
  void validateTenantId_validBaseTenants_doesNotThrow() {
    var provider = new SchemaSelectionConnectionProvider();

    // We expect UnsupportedOperationException for valid tenants since there is no pool running
    // during tests.
    assertThrows(UnsupportedOperationException.class, () -> provider.getTenantPool("njall_base"));
    assertThrows(UnsupportedOperationException.class, () -> provider.getTenantPool("njall_admin"));
    assertThrows(
        UnsupportedOperationException.class, () -> provider.getTenantPool("njall_analytics"));
  }

  @Test
  void validateTenantId_validServerTenant_doesNotThrow() {
    var provider = new SchemaSelectionConnectionProvider();

    // Test a valid server ID
    assertThrows(
        UnsupportedOperationException.class,
        () -> provider.getTenantPool("njall_server_testserver"));
  }
}
