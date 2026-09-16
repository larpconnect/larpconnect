package com.larpconnect.njall.data.domain;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

/**
 * Immutable representation of a server contact point.
 *
 * @param id The unique identifier of the contact point.
 * @param roleType The role classification for this contact.
 * @param contactType The communication channel type.
 * @param contact The contact address or identifier.
 * @param ordering Sort priority index for contact display.
 */
public record ServerContact(
    UUID id, RoleType roleType, ContactType contactType, String contact, int ordering) {

  public ServerContact {
    requireNonNull(id, "id cannot be null");
    requireNonNull(roleType, "roleType cannot be null");
    requireNonNull(contactType, "contactType cannot be null");
    requireNonNull(contact, "contact cannot be null");
  }

  /**
   * Pure factory method for creating a {@link ServerContact}.
   *
   * @param id The contact UUID.
   * @param roleType The role type.
   * @param contactType The contact type.
   * @param contact The contact address.
   * @param ordering The sort ordering index.
   * @return A new {@link ServerContact} instance.
   */
  public static ServerContact of(
      UUID id, RoleType roleType, ContactType contactType, String contact, int ordering) {
    return new ServerContact(id, roleType, contactType, contact, ordering);
  }
}
