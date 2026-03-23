package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TenantResolverTest {

  @Test
  void resolveCurrentTenantIdentifier_returnsPublic() {
    var resolver = new TenantResolver();
    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("public");
  }

  @Test
  void validateExistingCurrentSessions_returnsFalse() {
    var resolver = new TenantResolver();
    assertThat(resolver.validateExistingCurrentSessions()).isFalse();
  }
}
