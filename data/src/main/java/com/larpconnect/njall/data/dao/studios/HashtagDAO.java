package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.Hashtag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted hashtag operations in user space. */
public interface HashtagDAO extends DAO<Hashtag> {

  /**
   * Retrieves an active hashtag by studio tenant UUID and hashtag UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param hashtagId The hashtag UUID identifier.
   * @return An Optional containing the hashtag if found and active, otherwise empty.
   */
  Optional<Hashtag> findById(UUID tenantId, UUID hashtagId);

  /**
   * Retrieves an active hashtag by studio tenant UUID and case-insensitive tag name.
   *
   * @param tenantId The internal tenant UUID.
   * @param tag The tag name.
   * @return An Optional containing the hashtag if found and active, otherwise empty.
   */
  Optional<Hashtag> findByTag(UUID tenantId, String tag);

  /**
   * Lists all active hashtags for a studio tenant, ordered alphabetically by tag.
   *
   * @param tenantId The internal tenant UUID.
   * @return An ImmutableList of active hashtags.
   */
  ImmutableList<Hashtag> listAll(UUID tenantId);

  /**
   * Idempotently creates or resolves an active hashtag for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param tag The case-preserved tag name.
   * @param canonicalUrl The canonical URL path for the tag.
   * @param summary Optional human-readable description.
   * @return The created, resolved, or reactivated Hashtag domain record.
   */
  Hashtag create(UUID tenantId, String tag, String canonicalUrl, Optional<String> summary);

  /**
   * Idempotently provisions multiple hashtags in bulk for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param items The list of hashtag items (tag, canonicalUrl, summary).
   * @return An ImmutableList containing all resolved and created Hashtag records.
   */
  ImmutableList<Hashtag> batchCreate(UUID tenantId, List<TagCreationItem> items);

  /**
   * Applies partial updates to an active hashtag.
   *
   * @param tenantId The internal tenant UUID.
   * @param hashtagId The hashtag UUID identifier.
   * @param tag Optional updated tag string.
   * @param summary Optional updated summary.
   * @return An Optional containing the updated Hashtag if found and active, otherwise empty.
   */
  Optional<Hashtag> patch(
      UUID tenantId, UUID hashtagId, Optional<String> tag, Optional<String> summary);

  /**
   * Soft deletes a hashtag by updating its base entity deleted_on timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param hashtagId The hashtag UUID identifier.
   * @return true if the hashtag was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID hashtagId);

  /** Input carrier item for batch creation operations. */
  @Immutable
  record TagCreationItem(String tag, String canonicalUrl, Optional<String> summary) {}

  @Override
  default Optional<Hashtag> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct hashtag lookup requires tenant context; use findById(tenantId, hashtagId)");
  }
}
