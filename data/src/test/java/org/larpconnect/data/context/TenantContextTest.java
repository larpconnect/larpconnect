package org.larpconnect.data.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

public final class TenantContextTest {

  @Test
  public void testTenantContext() throws Exception {
    // Cover private constructor
    Constructor<TenantContext> constructor = TenantContext.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    assertThat(constructor.newInstance()).isNotNull();

    // Test ThreadLocal behavior
    TenantContext.clear();
    assertThat(TenantContext.getTenantSchema()).isNull();

    TenantContext.setTenantSchema("schema1");
    assertThat(TenantContext.getTenantSchema()).isEqualTo("schema1");

    TenantContext.clear();
    assertThat(TenantContext.getTenantSchema()).isNull();
  }
}
