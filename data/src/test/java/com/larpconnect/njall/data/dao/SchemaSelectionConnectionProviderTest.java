package com.larpconnect.njall.data.dao;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class SchemaSelectionConnectionProviderTest {

  @Test
  void getTenantPool_invalidTenant_throwsException() {
    var provider = new SchemaSelectionConnectionProvider();

    assertThrows(IllegalArgumentException.class, () -> provider.getTenantPool((String) null));
    assertThrows(IllegalArgumentException.class, () -> provider.getTenantPool(""));
    assertThrows(IllegalArgumentException.class, () -> provider.getTenantPool("some-tenant"));
  }

  @Test
  void getConnection_invalidTenant_throwsException() {
    var provider = new SchemaSelectionConnectionProvider();

    assertThrows(IllegalArgumentException.class, () -> provider.getConnection((String) null));
    assertThrows(IllegalArgumentException.class, () -> provider.getConnection(""));
    assertThrows(IllegalArgumentException.class, () -> provider.getConnection("some-tenant"));
  }
}
