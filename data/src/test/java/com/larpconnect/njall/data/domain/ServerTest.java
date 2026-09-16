package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerTest {

  private static final UUID SERVER_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.now();

  @Test
  @DisplayName("Server.of creates immutable Server with contacts")
  void of_validInputs_createsServer() {
    var contact =
        ServerContact.of(CONTACT_ID, RoleType.ADMIN, ContactType.EMAIL, "admin@larpconnect.org", 0);
    var server = Server.of(SERVER_ID, "node-1", "larpconnect.org", NOW, List.of(contact));

    assertThat(server.id()).isEqualTo(SERVER_ID);
    assertThat(server.name()).isEqualTo("node-1");
    assertThat(server.primaryDomain()).isEqualTo("larpconnect.org");
    assertThat(server.createdOn()).isEqualTo(NOW);
    assertThat(server.contacts()).containsExactly(contact);
  }

  @Test
  @DisplayName("Server throws NullPointerException when required fields are null")
  void of_nullFields_throwsNullPointerException() {
    var contact =
        ServerContact.of(CONTACT_ID, RoleType.ADMIN, ContactType.EMAIL, "admin@larpconnect.org", 0);

    assertThatThrownBy(() -> Server.of(null, "node-1", "larpconnect.org", NOW, List.of(contact)))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id cannot be null");

    assertThatThrownBy(() -> Server.of(SERVER_ID, null, "larpconnect.org", NOW, List.of(contact)))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name cannot be null");

    assertThatThrownBy(() -> Server.of(SERVER_ID, "node-1", null, NOW, List.of(contact)))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("primaryDomain cannot be null");

    assertThatThrownBy(
            () -> Server.of(SERVER_ID, "node-1", "larpconnect.org", null, List.of(contact)))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("createdOn cannot be null");

    assertThatThrownBy(() -> Server.of(SERVER_ID, "node-1", "larpconnect.org", NOW, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("contacts cannot be null");
  }

  @Test
  @DisplayName("ServerContact throws NullPointerException when required fields are null")
  void serverContact_nullFields_throwsNullPointerException() {
    assertThatThrownBy(
            () -> ServerContact.of(null, RoleType.ADMIN, ContactType.EMAIL, "admin@test.org", 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id cannot be null");

    assertThatThrownBy(
            () -> ServerContact.of(CONTACT_ID, null, ContactType.EMAIL, "admin@test.org", 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("roleType cannot be null");

    assertThatThrownBy(
            () -> ServerContact.of(CONTACT_ID, RoleType.ADMIN, null, "admin@test.org", 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("contactType cannot be null");

    assertThatThrownBy(
            () -> ServerContact.of(CONTACT_ID, RoleType.ADMIN, ContactType.EMAIL, null, 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("contact cannot be null");
  }
}
