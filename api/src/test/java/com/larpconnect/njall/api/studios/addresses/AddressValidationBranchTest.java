package com.larpconnect.njall.api.studios.addresses;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.domain.AddressType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AddressValidationBranchTest {

  @Test
  @DisplayName("validateCreate checks all field boundary rules")
  void validateCreate_allBoundaries() {
    var longLine1 =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "a".repeat(256), "Seattle", "WA", "98101", "US");
    assertThat(AddressActor.validateCreate(longLine1))
        .contains("addressLine1 must not exceed 255 characters");

    var blankLocality =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "  ", "WA", "98101", "US");
    assertThat(AddressActor.validateCreate(blankLocality)).contains("locality must not be blank");

    var blankAdminArea =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "   ", "98101", "US");
    assertThat(AddressActor.validateCreate(blankAdminArea))
        .contains("administrativeArea must not be blank");

    var blankPostal =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "  ", "US");
    assertThat(AddressActor.validateCreate(blankPostal)).contains("postalCode must not be blank");

    var longPostal =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "1".repeat(21), "US");
    assertThat(AddressActor.validateCreate(longPostal))
        .contains("postalCode must not exceed 20 characters");

    var invalidCountryShort =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "98101", "U");
    assertThat(AddressActor.validateCreate(invalidCountryShort))
        .contains("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");

    var invalidCountryLong =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "98101", "USA");
    assertThat(AddressActor.validateCreate(invalidCountryLong))
        .contains("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");

    var valid =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Main", "Seattle", "WA", "98101", "US");
    assertThat(AddressActor.validateCreate(valid)).isEmpty();
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
    assertThat(AddressActor.validatePatch(longLine1))
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
    assertThat(AddressActor.validatePatch(validLine1)).isEmpty();
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
    assertThat(AddressActor.validatePatch(blankPostal))
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
    assertThat(AddressActor.validatePatch(longPostal))
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
    assertThat(AddressActor.validatePatch(validPostal)).isEmpty();
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
    assertThat(AddressActor.validatePatch(invalidCountry))
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
    assertThat(AddressActor.validatePatch(validCountry)).isEmpty();

    assertThat(AddressActor.validatePatch(new UpdateAddressRequest())).isEmpty();
  }
}
