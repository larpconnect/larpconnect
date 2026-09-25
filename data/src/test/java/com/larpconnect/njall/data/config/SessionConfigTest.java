package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class SessionConfigTest {

  private static final String TEST_URL = "jdbc:postgresql://localhost:5432/db";

  @Test
  @DisplayName("Constructor creates SessionConfig with valid parameters")
  void constructor_validParameters_createsRecord() {
    var config = new SessionConfig(TEST_URL, "admin", "secret", 2, 10, 5);

    assertThat(config.jdbcUrl()).isEqualTo(TEST_URL);
    assertThat(config.username()).isEqualTo("admin");
    assertThat(config.password()).contains("secret");
    assertThat(config.trustAuth()).isFalse();
    assertThat(config.hasPassword()).isTrue();
    assertThat(config.minPoolSize()).isEqualTo(2);
    assertThat(config.maxPoolSize()).isEqualTo(10);
    assertThat(config.timeoutSeconds()).isEqualTo(5);
  }

  @Test
  @DisplayName("Constructor with explicit trustAuth allows null, empty, or blank password")
  void constructor_nullOrBlankPasswordWithTrustAuth_setsPasswordAndHasPasswordFalse() {
    var nullConfig = new SessionConfig(TEST_URL, "admin", (String) null, true, 2, 10, 5);
    assertThat(nullConfig.password()).isEmpty();
    assertThat(nullConfig.trustAuth()).isTrue();
    assertThat(nullConfig.hasPassword()).isFalse();

    var emptyConfig = new SessionConfig(TEST_URL, "admin", "", true, 2, 10, 5);
    assertThat(emptyConfig.password()).contains("");
    assertThat(emptyConfig.trustAuth()).isTrue();
    assertThat(emptyConfig.hasPassword()).isFalse();

    var blankConfig = new SessionConfig(TEST_URL, "admin", "   ", true, 2, 10, 5);
    assertThat(blankConfig.password()).contains("   ");
    assertThat(blankConfig.trustAuth()).isTrue();
    assertThat(blankConfig.hasPassword()).isFalse();
  }

  @Test
  @DisplayName("Constructor throws when password is null or blank and trustAuth is false")
  void constructor_nullOrBlankPasswordWithoutTrustAuth_throwsIllegalStateException() {
    assertThatThrownBy(() -> new SessionConfig(TEST_URL, "admin", null, 2, 10, 5))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Database password is required for profile with username 'admin'");

    assertThatThrownBy(() -> new SessionConfig(TEST_URL, "admin", "", false, 2, 10, 5))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Database password is required for profile with username 'admin'");

    assertThatThrownBy(() -> new SessionConfig(TEST_URL, "admin", "   ", false, 2, 10, 5))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Database password is required for profile with username 'admin'");
  }

  @Test
  @DisplayName("Constructor throws IllegalArgumentException when pool sizes are invalid")
  void constructor_invalidPoolSizes_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> new SessionConfig(TEST_URL, "admin", "secret", 0, 10, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minPoolSize must be at least 1");

    assertThatThrownBy(() -> new SessionConfig(TEST_URL, "admin", "secret", 10, 2, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxPoolSize must be greater than or equal to minPoolSize");

    assertThatThrownBy(() -> new SessionConfig(TEST_URL, "admin", "secret", 2, 10, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timeoutSeconds must be at least 1");
  }
}
