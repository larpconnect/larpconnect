package com.larpconnect.njall.api.studios.addresses;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AddressValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new GuavaModule());

  @Test
  @DisplayName("AddressValidation handles create and patch rules")
  void addressValidation_rules() {
    var blankLine1 =
        new CreateAddressRequest(AddressType.PHYSICAL, "", "Pineville", "WA", "98101", "US");
    assertThat(AddressActor.validateCreate(blankLine1)).contains("addressLine1 must not be blank");

    var invalidCountry =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "123 Camp Rd", "Pineville", "WA", "98101", "USA");
    assertThat(AddressActor.validateCreate(invalidCountry))
        .contains("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");

    var validCreate =
        new CreateAddressRequest(
            AddressType.PHYSICAL,
            "123 Camp Rd",
            "Site B",
            null,
            "Pineville",
            "WA",
            "98101",
            "US",
            new GeoJsonPoint(-122.33, 47.60));
    assertThat(AddressActor.validateCreate(validCreate)).isEmpty();

    var blankPatchLine1 =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.of(""),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(AddressActor.validatePatch(blankPatchLine1))
        .contains("addressLine1 must not be blank");
  }

  @Test
  @DisplayName("Address DTOs serialize and deserialize GeoJSON Points correctly")
  void addressDtos_serialization() throws Exception {
    var createJson =
        """
        {
          "addressType": "PHYSICAL",
          "addressLine1": "123 Forest Rd",
          "locality": "Pineville",
          "administrativeArea": "WA",
          "postalCode": "98101",
          "countryCode": "US",
          "geom": {
            "type": "Point",
            "coordinates": [-122.3321, 47.6062]
          }
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateAddressRequest.class);
    assertThat(createReq.addressType()).isEqualTo(AddressType.PHYSICAL);
    assertThat(createReq.addressLine1()).isEqualTo("123 Forest Rd");
    assertThat(createReq.geom()).isPresent();
    assertThat(createReq.geom().orElseThrow().longitude()).isEqualTo(-122.3321);
    assertThat(createReq.geom().orElseThrow().latitude()).isEqualTo(47.6062);

    var addressId = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var res =
        new AddressResponse(
            addressId,
            locationId,
            AddressType.PHYSICAL,
            "123 Forest Rd",
            Optional.empty(),
            Optional.empty(),
            "Pineville",
            "WA",
            "98101",
            "US",
            createReq.geom());
    var resJson = objectMapper.writeValueAsString(res);
    assertThat(resJson).contains(addressId.toString());
    assertThat(resJson).contains("-122.3321");
    assertThat(resJson).contains("47.6062");
    assertThat(resJson).doesNotContain("tenantId");
  }
}
