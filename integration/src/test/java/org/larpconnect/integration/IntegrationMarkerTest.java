package org.larpconnect.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Integration test suite skeleton and marker verification. */
public final class IntegrationMarkerTest {
  @Test
  public void isActive_withMarker_returnsTrue() {
    IntegrationMarker marker = new IntegrationMarker();
    assertThat(marker.isActive()).isTrue();
  }
}
