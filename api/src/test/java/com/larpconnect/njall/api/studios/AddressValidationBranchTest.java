package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.domain.AddressType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AddressValidationBranchTest {

  @Test
  @DisplayName("validateCreate checks all field boundary rules")
  void validateCreate_allBoundaries() {
    var longLine1 =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "a".repeat(256), "Seattle", "WA", "98101", "US");
    assertThat(AddressValidation.validateCreate(longLine1))
        .contains("addressLine1 must not exceed 255 characters");

    var blankLocality =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "  ", "WA", "98101", "US");
    assertThat(AddressValidation.validateCreate(blankLocality))
        .contains("locality must not be blank");

    var blankAdminArea =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "   ", "98101", "US");
    assertThat(AddressValidation.validateCreate(blankAdminArea))
        .contains("administrativeArea must not be blank");

    var blankPostal =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "  ", "US");
    assertThat(AddressValidation.validateCreate(blankPostal))
        .contains("postalCode must not be blank");

    var longPostal =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "1".repeat(21), "US");
    assertThat(AddressValidation.validateCreate(longPostal))
        .contains("postalCode must not exceed 20 characters");

    var invalidCountryShort =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "98101", "U");
    assertThat(AddressValidation.validateCreate(invalidCountryShort))
        .contains("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");

    var invalidCountryLong =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "98101", "USA");
    assertThat(AddressValidation.validateCreate(invalidCountryLong))
        .contains("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");

    var valid =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "98101", "US");
    assertThat(AddressValidation.validateCreate(valid)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch checks addressLine1 boundary rules")
  void validatePatch_addressLine1() {
    var longLine1 =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.of("a".repeat(256)),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(longLine1))
        .contains("addressLine1 must not exceed 255 characters");

    var validLine1 =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.of("123 Valid"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(validLine1)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch checks postalCode boundary rules")
  void validatePatch_postalCode() {
    var blankPostal =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("   "),
            Optional.empty(),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(blankPostal))
        .contains("postalCode must not exceed 20 characters");

    var longPostal =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("1".repeat(21)),
            Optional.empty(),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(longPostal))
        .contains("postalCode must not exceed 20 characters");

    var validPostal =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("98101"),
            Optional.empty(),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(validPostal)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch checks countryCode boundary rules and empty request")
  void validatePatch_countryCodeAndEmpty() {
    var invalidCountry =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("USA"),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(invalidCountry))
        .contains("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");

    var validCountry =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("CA"),
            Optional.empty());
    assertThat(AddressValidation.validatePatch(validCountry)).isEmpty();

    assertThat(AddressValidation.validatePatch(new UpdateAddressRequest())).isEmpty();
  }

  @Test
  @DisplayName("LocationValidation checks patch name length and tryParseUuid")
  void locationValidation_boundaries() {
    var longPatch = new UpdateLocationRequest("b".repeat(256), null);
    assertThat(LocationValidation.validatePatch(longPatch))
        .contains("Location name must not exceed 255 characters");

    assertThat(LocationValidation.tryParseUuid(null)).isEmpty();
    assertThat(LocationValidation.tryParseUuid("")).isEmpty();
    assertThat(LocationValidation.tryParseUuid("not-a-uuid")).isEmpty();
    var validUuid = UUID.randomUUID();
    assertThat(LocationValidation.tryParseUuid(validUuid.toString())).contains(validUuid);
  }
}
