package com.larpconnect.njall.api.studios;

import java.util.Optional;

final class AddressValidation {

  private AddressValidation() {}

  static Optional<String> validateCreate(CreateAddressRequest request) {
    var lineErr = validateAddressLine1(request.addressLine1());
    if (lineErr.isPresent()) {
      return lineErr;
    }
    var areaErr = validateArea(request.locality(), request.administrativeArea());
    if (areaErr.isPresent()) {
      return areaErr;
    }
    return validatePostalAndCountry(request.postalCode(), request.countryCode());
  }

  static Optional<String> validatePatch(UpdateAddressRequest request) {
    if (request.addressLine1().isPresent()) {
      var err = validateAddressLine1(request.addressLine1().get());
      if (err.isPresent()) {
        return err;
      }
    }
    if (request.postalCode().isPresent()) {
      var code = request.postalCode().get();
      if (code.isBlank() || code.length() > 20) {
        return Optional.of("postalCode must not exceed 20 characters");
      }
    }
    if (request.countryCode().isPresent() && request.countryCode().get().trim().length() != 2) {
      return Optional.of("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");
    }
    return Optional.empty();
  }

  private static Optional<String> validateAddressLine1(String addressLine1) {
    if (addressLine1.isBlank()) {
      return Optional.of("addressLine1 must not be blank");
    }
    if (addressLine1.length() > 255) {
      return Optional.of("addressLine1 must not exceed 255 characters");
    }
    return Optional.empty();
  }

  private static Optional<String> validateArea(String locality, String administrativeArea) {
    if (locality.isBlank()) {
      return Optional.of("locality must not be blank");
    }
    if (administrativeArea.isBlank()) {
      return Optional.of("administrativeArea must not be blank");
    }
    return Optional.empty();
  }

  private static Optional<String> validatePostalAndCountry(String postalCode, String countryCode) {
    if (postalCode.isBlank()) {
      return Optional.of("postalCode must not be blank");
    }
    if (postalCode.length() > 20) {
      return Optional.of("postalCode must not exceed 20 characters");
    }
    if (countryCode.trim().length() != 2) {
      return Optional.of("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");
    }
    return Optional.empty();
  }
}
