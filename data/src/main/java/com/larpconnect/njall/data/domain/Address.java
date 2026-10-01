package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of an address associated with a location.
 *
 * @param id The unique address UUID identifier.
 * @param locationId The parent location UUID identifier.
 * @param addressType The functional classification of the address.
 * @param addressLine1 Primary street address line.
 * @param addressLine2 Optional secondary street address line.
 * @param addressLine3 Optional tertiary street address line.
 * @param locality City, town, or municipality.
 * @param administrativeArea State, province, or region.
 * @param postalCode Postal code or ZIP code.
 * @param countryCode Two-letter ISO 3166-1 alpha-2 country code.
 * @param geom Optional PostGIS geographic point geometry.
 */
@Immutable
public record Address(
    UUID id,
    UUID locationId,
    AddressType addressType,
    String addressLine1,
    Optional<String> addressLine2,
    Optional<String> addressLine3,
    String locality,
    String administrativeArea,
    String postalCode,
    String countryCode,
    Optional<GeoJsonPoint> geom)
    implements DatabaseObject {

  public Address(
      UUID id,
      UUID locationId,
      AddressType addressType,
      String addressLine1,
      @Nullable String addressLine2,
      @Nullable String addressLine3,
      String locality,
      String administrativeArea,
      String postalCode,
      String countryCode,
      @Nullable GeoJsonPoint geom) {
    this(
        id,
        locationId,
        addressType,
        addressLine1,
        Optional.ofNullable(addressLine2).filter(s -> !s.isBlank()),
        Optional.ofNullable(addressLine3).filter(s -> !s.isBlank()),
        locality,
        administrativeArea,
        postalCode,
        countryCode,
        Optional.ofNullable(geom));
  }
}
