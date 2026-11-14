package com.larpconnect.njall.data.dao.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class PatchCampaignParamsTest {

  @Test
  @DisplayName("Three-arg PatchCampaignParams constructor defaults clear flags to false")
  void convenienceConstructor_defaultsClearFlagsToFalse() {
    var systemId = UUID.randomUUID();
    var params =
        new PatchCampaignParams(
            Optional.of("Updated Title"), Optional.of("Updated Summary"), Optional.of(systemId));

    assertThat(params.title()).contains("Updated Title");
    assertThat(params.summary()).contains("Updated Summary");
    assertThat(params.clearSummary()).isFalse();
    assertThat(params.systemId()).contains(systemId);
    assertThat(params.clearSystem()).isFalse();
  }

  @Test
  @DisplayName("Canonical PatchCampaignParams constructor preserves explicit clear flags")
  void canonicalConstructor_preservesClearFlags() {
    var params =
        new PatchCampaignParams(Optional.empty(), Optional.empty(), true, Optional.empty(), true);

    assertThat(params.title()).isEmpty();
    assertThat(params.summary()).isEmpty();
    assertThat(params.clearSummary()).isTrue();
    assertThat(params.systemId()).isEmpty();
    assertThat(params.clearSystem()).isTrue();
  }
}
