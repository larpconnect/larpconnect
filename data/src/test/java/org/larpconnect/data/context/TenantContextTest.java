package org.larpconnect.data.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public final class TenantContextTest {

  @Test
  public void getTenantSchema_defaultSupplier_returnsDefaultSchema() {
    assertThat(TenantContext.getTenantSchema()).isEqualTo("njall_core_default");
  }

  @Test
  public void getTenantSchema_customSupplier_returnsCustomSchema() {
    TenantContext.setTenantSupplier(() -> "custom_schema");
    try {
      assertThat(TenantContext.getTenantSchema()).isEqualTo("custom_schema");
    } finally {
      // Restore default
      TenantContext.setTenantSupplier(() -> "njall_core_default");
    }
  }
}
