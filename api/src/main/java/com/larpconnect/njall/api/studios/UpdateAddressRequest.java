package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a subordinate address (AIP-134). */
@Immutable
public record UpdateAddressRequest(
    @JsonProperty("addressType") Optional<AddressType> addressType,
    @JsonProperty("addressLine1") Optional<String> addressLine1,
    @JsonProperty("addressLine2") Optional<String> addressLine2,
    @JsonProperty("addressLine3") Optional<String> addressLine3,
    @JsonProperty("locality") Optional<String> locality,
    @JsonProperty("administrativeArea") Optional<String> administrativeArea,
    @JsonProperty("postalCode") Optional<String> postalCode,
    @JsonProperty("countryCode") Optional<String> countryCode,
    @JsonProperty("geom") Optional<GeoJsonPoint> geom) {

  public UpdateAddressRequest() {
    this(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @JsonCreator
  public UpdateAddressRequest(
      @JsonProperty("addressType") @Nullable AddressType addressType,
      @JsonProperty("addressLine1") @Nullable String addressLine1,
      @JsonProperty("addressLine2") @Nullable String addressLine2,
      @JsonProperty("addressLine3") @Nullable String addressLine3,
      @JsonProperty("locality") @Nullable String locality,
      @JsonProperty("administrativeArea") @Nullable String administrativeArea,
      @JsonProperty("postalCode") @Nullable String postalCode,
      @JsonProperty("countryCode") @Nullable String countryCode,
      @JsonProperty("geom") @Nullable GeoJsonPoint geom) {
    this(
        Optional.ofNullable(addressType),
        Optional.ofNullable(addressLine1),
        Optional.ofNullable(addressLine2),
        Optional.ofNullable(addressLine3),
        Optional.ofNullable(locality),
        Optional.ofNullable(administrativeArea),
        Optional.ofNullable(postalCode),
        Optional.ofNullable(countryCode),
        Optional.ofNullable(geom));
  }
}
