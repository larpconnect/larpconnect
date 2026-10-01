package com.larpconnect.njall.data.domain;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.util.List;

/**
 * Immutable domain representation of an RFC 7946 GeoJSON Point geometry.
 *
 * @param type The GeoJSON geometry type, strictly "Point".
 * @param coordinates Two-element list containing [longitude, latitude].
 */
@Immutable
public record GeoJsonPoint(String type, ImmutableList<Double> coordinates) {

  private static final String TYPE_POINT = "Point";
  private static final int COORDINATE_COUNT = 2;
  private static final double MIN_LONGITUDE = -180.0;
  private static final double MAX_LONGITUDE = 180.0;
  private static final double MIN_LATITUDE = -90.0;
  private static final double MAX_LATITUDE = 90.0;

  /**
   * Compact constructor validating RFC 7946 invariants and geographic boundaries.
   *
   * @throws IllegalArgumentException if type is not "Point", coordinates size != 2, or coordinates
   *     exceed valid geographic bounds.
   */
  public GeoJsonPoint {
    if (!TYPE_POINT.equals(type)) {
      throw new IllegalArgumentException("GeoJSON type must be 'Point', got: " + type);
    }
    if (coordinates.size() != COORDINATE_COUNT) {
      throw new IllegalArgumentException(
          "GeoJSON Point coordinates must contain exactly 2 numbers [longitude, latitude]");
    }
    double lon = coordinates.get(0);
    double lat = coordinates.get(1);
    if (lon < MIN_LONGITUDE || lon > MAX_LONGITUDE) {
      throw new IllegalArgumentException(
          "Longitude must be between -180.0 and 180.0 degrees, got: " + lon);
    }
    if (lat < MIN_LATITUDE || lat > MAX_LATITUDE) {
      throw new IllegalArgumentException(
          "Latitude must be between -90.0 and 90.0 degrees, got: " + lat);
    }
  }

  /**
   * Creates a GeoJSON Point from explicit longitude and latitude coordinates.
   *
   * @param longitude The geographic longitude in degrees [-180, 180].
   * @param latitude The geographic latitude in degrees [-90, 90].
   */
  public GeoJsonPoint(double longitude, double latitude) {
    this(TYPE_POINT, ImmutableList.of(longitude, latitude));
  }

  /**
   * Creates a GeoJSON Point from a coordinate list.
   *
   * @param coordinates The coordinate list [longitude, latitude].
   */
  public GeoJsonPoint(List<Double> coordinates) {
    this(TYPE_POINT, ImmutableList.copyOf(coordinates));
  }

  /** Returns the longitude (X coordinate) of the point. */
  public double longitude() {
    return coordinates.get(0);
  }

  /** Returns the latitude (Y coordinate) of the point. */
  public double latitude() {
    return coordinates.get(1);
  }
}
