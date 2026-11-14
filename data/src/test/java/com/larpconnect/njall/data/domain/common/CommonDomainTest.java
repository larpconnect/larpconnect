package com.larpconnect.njall.data.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CommonDomainTest {

  @Test
  @DisplayName("DeletionFilter includesDeleted reflects enum constant")
  void deletionFilter_includesDeleted() {
    assertThat(DeletionFilter.ACTIVE_ONLY.includesDeleted()).isFalse();
    assertThat(DeletionFilter.INCLUDE_DELETED.includesDeleted()).isTrue();
  }

  @Test
  @DisplayName("GeoJsonPoint validates valid coordinates and provides accessors")
  void geoJsonPoint_validCoordinates() {
    var point = new GeoJsonPoint(-122.3321, 47.6062);
    assertThat(point.type()).isEqualTo("Point");
    assertThat(point.longitude()).isEqualTo(-122.3321);
    assertThat(point.latitude()).isEqualTo(47.6062);
    assertThat(point.coordinates()).containsExactly(-122.3321, 47.6062);

    var pointFromList = new GeoJsonPoint(List.of(10.0, 20.0));
    assertThat(pointFromList.longitude()).isEqualTo(10.0);
    assertThat(pointFromList.latitude()).isEqualTo(20.0);
  }

  @Test
  @DisplayName("GeoJsonPoint rejects invalid types, sizes, and boundary violations")
  void geoJsonPoint_invalidInputs() {
    assertThatThrownBy(() -> new GeoJsonPoint("Polygon", ImmutableList.of(-122.0, 47.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type must be 'Point'");

    assertThatThrownBy(() -> new GeoJsonPoint("Point", ImmutableList.of(-122.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("contain exactly 2 numbers");

    assertThatThrownBy(() -> new GeoJsonPoint(-181.0, 45.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Longitude must be between -180.0 and 180.0");

    assertThatThrownBy(() -> new GeoJsonPoint(181.0, 45.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Longitude must be between -180.0 and 180.0");

    assertThatThrownBy(() -> new GeoJsonPoint(0.0, -91.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Latitude must be between -90.0 and 90.0");

    assertThatThrownBy(() -> new GeoJsonPoint(0.0, 91.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Latitude must be between -90.0 and 90.0");
  }
}
