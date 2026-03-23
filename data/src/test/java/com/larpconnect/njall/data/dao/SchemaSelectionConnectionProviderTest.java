package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class SchemaSelectionConnectionProviderTest {

  @Test
  void getTenantPool_emptyOrPublicTenant_returnsSuper() {
    var provider = new SchemaSelectionConnectionProvider();

    assertThat(provider.getTenantPool(null)).isNull();
    assertThat(provider.getTenantPool("")).isNull();
    assertThat(provider.getTenantPool("public")).isNull();
    assertThat(provider.getTenantPool("some-tenant")).isNull();
  }
}
