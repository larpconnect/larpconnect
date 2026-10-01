package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import java.util.UUID;

/** Public response for a subordinate location address. */
@Immutable
public record AddressResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("locationId") UUID locationId,
    @JsonProperty("addressType") AddressType addressType,
    @JsonProperty("addressLine1") String addressLine1,
    @JsonProperty("addressLine2") Optional<String> addressLine2,
    @JsonProperty("addressLine3") Optional<String> addressLine3,
    @JsonProperty("locality") String locality,
    @JsonProperty("administrativeArea") String administrativeArea,
    @JsonProperty("postalCode") String postalCode,
    @JsonProperty("countryCode") String countryCode,
    @JsonProperty("geom") Optional<GeoJsonPoint> geom) {}
