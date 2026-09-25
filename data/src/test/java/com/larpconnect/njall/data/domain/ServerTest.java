package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
  @DisplayName("Constructor creates immutable Server with contacts")
  void constructor_validInputs_createsServer() {
    var contact =
        new ServerContact(
            CONTACT_ID, RoleType.ADMIN, ContactType.EMAIL, "admin@larpconnect.org", 0);
    var server = new Server(SERVER_ID, "node-1", "larpconnect.org", NOW, List.of(contact));

    assertThat(server.id()).isEqualTo(SERVER_ID);
    assertThat(server.name()).isEqualTo("node-1");
    assertThat(server.primaryDomain()).isEqualTo("larpconnect.org");
    assertThat(server.createdOn()).isEqualTo(NOW);
    assertThat(server.contacts()).containsExactly(contact);
  }

  @Test
  @DisplayName("Constructor creates valid contact record")
  void serverContact_constructor_createsRecord() {
    var contact =
        new ServerContact(
            CONTACT_ID, RoleType.ADMIN, ContactType.EMAIL, "admin@larpconnect.org", 0);

    assertThat(contact.id()).isEqualTo(CONTACT_ID);
    assertThat(contact.roleType()).isEqualTo(RoleType.ADMIN);
    assertThat(contact.contactType()).isEqualTo(ContactType.EMAIL);
    assertThat(contact.contact()).isEqualTo("admin@larpconnect.org");
    assertThat(contact.ordering()).isEqualTo(0);
  }
}
