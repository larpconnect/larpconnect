package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminValidationTest {

  @Test
  @DisplayName("isValidIdentifier validates lowercase alphanumeric starting with letter")
  void isValidIdentifier_validAndInvalidCases() {
    assertThat(AdminValidation.isValidIdentifier("valhalla")).isTrue();
    assertThat(AdminValidation.isValidIdentifier("studio_1")).isTrue();
    assertThat(AdminValidation.isValidIdentifier("admin_role_2")).isTrue();

    assertThat(AdminValidation.isValidIdentifier(null)).isFalse();
    assertThat(AdminValidation.isValidIdentifier("")).isFalse();
    assertThat(AdminValidation.isValidIdentifier("1studio")).isFalse();
    assertThat(AdminValidation.isValidIdentifier("_role")).isFalse();
    assertThat(AdminValidation.isValidIdentifier("Valhalla")).isFalse();
    assertThat(AdminValidation.isValidIdentifier("role-name")).isFalse();
    assertThat(AdminValidation.isValidIdentifier("role name")).isFalse();
  }

  @Test
  @DisplayName("tryParseUuid parses valid UUID and returns empty for invalid")
  void tryParseUuid_validAndInvalidCases() {
    var uuid = UUID.randomUUID();
    assertThat(AdminValidation.tryParseUuid(uuid.toString())).contains(uuid);

    assertThat(AdminValidation.tryParseUuid(null)).isEmpty();
    assertThat(AdminValidation.tryParseUuid("not-a-uuid")).isEmpty();
    assertThat(AdminValidation.tryParseUuid("valhalla")).isEmpty();
  }
}
