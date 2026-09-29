package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.Link;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted external link operations in user space. */
public non-sealed interface LinkDAO extends DAO<Link> {

  /**
   * Retrieves an active link by studio tenant UUID and link UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param linkId The link UUID identifier.
   * @return An Optional containing the link if found and active, otherwise empty.
   */
  Optional<Link> findById(UUID tenantId, UUID linkId);

  /**
   * Persists a new link and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param linkType The semantic type of the link.
   * @param url The destination URL.
   * @param mediaType The IANA media type.
   * @param summary Optional human-readable description.
   * @return The persisted Link domain record.
   */
  Link create(
      UUID tenantId, String linkType, String url, String mediaType, Optional<String> summary);

  /**
   * Applies partial updates to an active link.
   *
   * @param tenantId The internal tenant UUID.
   * @param linkId The link UUID identifier.
   * @param linkType Optional updated link type.
   * @param url Optional updated destination URL.
   * @param mediaType Optional updated media type.
   * @param summary Optional updated summary.
   * @return An Optional containing the updated Link if found and active, otherwise empty.
   */
  Optional<Link> patch(
      UUID tenantId,
      UUID linkId,
      Optional<String> linkType,
      Optional<String> url,
      Optional<String> mediaType,
      Optional<String> summary);

  /**
   * Soft deletes a link by updating its base entity deleted_on timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param linkId The link UUID identifier.
   * @return true if the link was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID linkId);

  @Override
  default Optional<Link> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct link lookup requires tenant context; use findById(tenantId, linkId)");
  }
}
