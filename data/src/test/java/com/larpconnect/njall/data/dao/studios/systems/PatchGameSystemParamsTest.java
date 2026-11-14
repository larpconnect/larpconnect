package com.larpconnect.njall.data.dao.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class PatchGameSystemParamsTest {

  @Test
  @DisplayName("Two-arg constructor defaults clearSummary to false")
  void twoArgConstructor_defaultsClearSummaryToFalse() {
    var params = createParams(Optional.of("Pathfinder 2e"), Optional.of("D20 fantasy system"));

    assertThat(params.name()).contains("Pathfinder 2e");
    assertThat(params.summary()).contains("D20 fantasy system");
    assertThat(params.clearSummary()).isFalse();
  }

  @Test
  @DisplayName("Canonical constructor preserves clearSummary when set to true")
  void canonicalConstructor_preservesClearSummaryTrue() {
    var params = createParamsWithClear(Optional.empty(), Optional.empty(), true);

    assertThat(params.name()).isEmpty();
    assertThat(params.summary()).isEmpty();
    assertThat(params.clearSummary()).isTrue();
  }

  private static PatchGameSystemParams createParams(
      Optional<String> name, Optional<String> summary) {
    return new PatchGameSystemParams(name, summary);
  }

  private static PatchGameSystemParams createParamsWithClear(
      Optional<String> name, Optional<String> summary, boolean clearSummary) {
    return new PatchGameSystemParams(name, summary, clearSummary);
  }
}
