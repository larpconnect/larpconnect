package com.larpconnect.njall.api.studios.addresses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a subordinate address under a location. */
@Immutable
public record CreateAddressRequest(
    @JsonProperty("addressType") AddressType addressType,
    @JsonProperty("addressLine1") String addressLine1,
    @JsonProperty("addressLine2") Optional<String> addressLine2,
    @JsonProperty("addressLine3") Optional<String> addressLine3,
    @JsonProperty("locality") String locality,
    @JsonProperty("administrativeArea") String administrativeArea,
    @JsonProperty("postalCode") String postalCode,
    @JsonProperty("countryCode") String countryCode,
    @JsonProperty("geom") Optional<GeoJsonPoint> geom) {

  public CreateAddressRequest(
      AddressType addressType,
      String addressLine1,
      String locality,
      String administrativeArea,
      String postalCode,
      String countryCode) {
    this(
        addressType,
        addressLine1,
        Optional.empty(),
        Optional.empty(),
        locality,
        administrativeArea,
        postalCode,
        countryCode,
        Optional.empty());
  }

  @JsonCreator
  public CreateAddressRequest(
      @JsonProperty("addressType") AddressType addressType,
      @JsonProperty("addressLine1") String addressLine1,
      @JsonProperty("addressLine2") @Nullable String addressLine2,
      @JsonProperty("addressLine3") @Nullable String addressLine3,
      @JsonProperty("locality") String locality,
      @JsonProperty("administrativeArea") String administrativeArea,
      @JsonProperty("postalCode") String postalCode,
      @JsonProperty("countryCode") String countryCode,
      @JsonProperty("geom") @Nullable GeoJsonPoint geom) {
    this(
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
