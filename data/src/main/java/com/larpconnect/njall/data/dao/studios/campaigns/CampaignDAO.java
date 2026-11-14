package com.larpconnect.njall.data.dao.studios.campaigns;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.studios.Campaign;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted campaign operations in user space. */
public interface CampaignDAO extends DAO<Campaign> {

  /**
   * Retrieves an active campaign by studio tenant UUID and campaign UUID.
   *
   * <p>If the campaign references a soft-deleted {@code System} entity, {@code systemId} is masked
   * as {@link Optional#empty()}.
   *
   * @param tenantId The internal tenant UUID.
   * @param campaignId The campaign UUID identifier.
   * @return An Optional containing the campaign if found, active, and of type {@code Campaign}.
   */
  Optional<Campaign> findById(UUID tenantId, UUID campaignId);

  /**
   * Retrieves all active campaigns for the specified studio tenant, optionally filtered by active
   * {@code systemId}, ordered by {@code title ASC, created_on DESC}.
   *
   * <p>Campaigns referencing a soft-deleted {@code System} have {@code systemId} masked as {@link
   * Optional#empty()}, and filtering by a soft-deleted or nonexistent {@code systemId} returns an
   * empty list.
   *
   * @param tenantId The internal tenant UUID.
   * @param systemId Optional active game system UUID filter.
   * @return An immutable list of matching active campaigns.
   */
  ImmutableList<Campaign> listAll(UUID tenantId, Optional<UUID> systemId);

  /**
   * Retrieves all active campaigns for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @return An immutable list of matching active campaigns.
   */
  default ImmutableList<Campaign> listAll(UUID tenantId) {
    return listAll(tenantId, Optional.empty());
  }

  /**
   * Persists a new campaign and its base entity for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param generator The generating actor entity UUID.
   * @param title The required campaign title.
   * @param summary Optional description summary from the base entity.
   * @param systemId Optional active game system UUID identifier.
   * @return The persisted Campaign domain record.
   * @throws IllegalArgumentException if {@code generator} is not an active Actor or {@code
   *     systemId} does not reference an active System in {@code tenantId}.
   */
  Campaign create(
      UUID tenantId,
      UUID generator,
      String title,
      Optional<String> summary,
      Optional<UUID> systemId);

  /**
   * Applies partial updates to an active campaign.
   *
   * @param tenantId The internal tenant UUID.
   * @param campaignId The campaign UUID identifier.
   * @param params Partial field updates and explicit null clearing flags.
   * @return An Optional containing the updated Campaign if found, active, and of type {@code
   *     Campaign}.
   * @throws IllegalArgumentException if {@code params.systemId()} does not reference an active
   *     System in {@code tenantId} when {@code params.clearSystem()} is false.
   */
  Optional<Campaign> patch(UUID tenantId, UUID campaignId, PatchCampaignParams params);

  /**
   * Soft deletes a campaign by updating its base entity {@code deleted_on} timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param campaignId The campaign UUID identifier.
   * @return true if the active {@code Campaign} entity was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID campaignId);

  @Override
  default Optional<Campaign> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct campaign lookup requires tenant context; use findById(tenantId, campaignId)");
  }

  @Override
  default ImmutableList<Campaign> list() {
    throw new UnsupportedOperationException(
        "Direct un-tenanted campaign listing is not permitted in user space; use listAll(tenantId,"
            + " systemId)");
  }
}
