package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
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
@Immutable
public record ServerContact(
    UUID id, RoleType roleType, ContactType contactType, String contact, int ordering) {}
