package com.larpconnect.njall.data.domain;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable domain representation of a registered Njall server and its contact points.
 *
 * @param id The server UUID.
 * @param name The unique human-readable server name.
 * @param primaryDomain The primary canonical domain name.
 * @param createdOn Timestamp when the server record was provisioned.
 * @param contacts List of administrative and operational contact records.
 */
public record Server(
    UUID id,
    String name,
    String primaryDomain,
    Instant createdOn,
    ImmutableList<ServerContact> contacts)
    implements DatabaseObject {

  public Server {
    requireNonNull(id, "id cannot be null");
    requireNonNull(name, "name cannot be null");
    requireNonNull(primaryDomain, "primaryDomain cannot be null");
    requireNonNull(createdOn, "createdOn cannot be null");
    requireNonNull(contacts, "contacts cannot be null");
    contacts = ImmutableList.copyOf(contacts);
  }

  /**
   * Constructs a {@link Server} from a generic {@link List} of contacts.
   *
   * @param id The server UUID.
   * @param name The server name.
   * @param primaryDomain The primary domain name.
   * @param createdOn The creation timestamp.
   * @param contacts The list of contacts.
   */
  public Server(
      UUID id, String name, String primaryDomain, Instant createdOn, List<ServerContact> contacts) {
    this(
        id,
        name,
        primaryDomain,
        createdOn,
        ImmutableList.copyOf(requireNonNull(contacts, "contacts cannot be null")));
  }

  /**
   * Pure factory method for creating a {@link Server} from a {@link List} of contacts.
   *
   * @param id The server UUID.
   * @param name The server name.
   * @param primaryDomain The primary domain name.
   * @param createdOn The creation timestamp.
   * @param contacts The associated contacts.
   * @return A new {@link Server} instance.
   */
  public static Server of(
      UUID id, String name, String primaryDomain, Instant createdOn, List<ServerContact> contacts) {
    return new Server(id, name, primaryDomain, createdOn, contacts);
  }
}
