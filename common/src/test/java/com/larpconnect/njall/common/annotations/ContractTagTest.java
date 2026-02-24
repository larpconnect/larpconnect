package com.larpconnect.njall.common.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContractTagTest {

  @Test
  void should_haveExpectedValues() {
    assertThat(ContractTag.values()).containsExactly(ContractTag.PURE, ContractTag.IDEMPOTENT);
  }

  @Test
  void should_parseValues() {
    assertThat(ContractTag.valueOf("PURE")).isEqualTo(ContractTag.PURE);
    assertThat(ContractTag.valueOf("IDEMPOTENT")).isEqualTo(ContractTag.IDEMPOTENT);
  }
}
