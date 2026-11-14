package com.larpconnect.njall.data.dao.studios.games;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.studios.Game;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted 2nd-level CTI game operations in user space. */
public interface GameDAO extends DAO<Game> {

  /**
   * Retrieves an active game by studio tenant UUID and game UUID, masking soft-deleted campaign
   * references while preserving historical system identifiers.
   *
   * @param tenantId The internal tenant UUID.
   * @param gameId The game UUID identifier.
   * @return An Optional containing the game if found, active, and of type {@code Game}.
   */
  Optional<Game> findById(UUID tenantId, UUID gameId);

  /**
   * Retrieves active games for the specified studio tenant matching optional system and campaign
   * filters, ordered by {@code start_time ASC NULLS LAST, created_on DESC}.
   *
   * @param tenantId The internal tenant UUID.
   * @param systemId Optional system UUID filter.
   * @param campaignId Optional active campaign UUID filter.
   * @return An immutable list of matching active games with soft-deleted campaigns masked.
   */
  ImmutableList<Game> listAll(UUID tenantId, Optional<UUID> systemId, Optional<UUID> campaignId);

  /**
   * Retrieves all active games for the specified studio tenant without filters.
   *
   * @param tenantId The internal tenant UUID.
   * @return An immutable list of all active games for the tenant.
   */
  default ImmutableList<Game> listAll(UUID tenantId) {
    return listAll(tenantId, Optional.empty(), Optional.empty());
  }

  /**
   * Atomically persists a new 2nd-level CTI game ({@code EntityBaseEntity}, {@code EventEntity},
   * and {@code GameEntity}) for the specified studio tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param params The game creation parameters.
   * @return The persisted Game domain record.
   * @throws IllegalArgumentException if generator is not an active Actor, system is not an active
   *     System, campaign (if present) is not an active Campaign, location (if present) is not an
   *     active Location, or {@code endTime} precedes {@code startTime}.
   */
  Game create(UUID tenantId, CreateGameParams params);

  /**
   * Applies partial updates to an active game.
   *
   * @param tenantId The internal tenant UUID.
   * @param gameId The game UUID identifier.
   * @param params Partial field updates and explicit null clearing flags.
   * @return An Optional containing the updated Game if found, active, and of type {@code Game}.
   * @throws IllegalArgumentException if referenced system/campaign/location is invalid or inactive,
   *     or the resulting {@code endTime} precedes {@code startTime}.
   */
  Optional<Game> patch(UUID tenantId, UUID gameId, PatchGameParams params);

  /**
   * Soft deletes a game by updating its base entity {@code deleted_on} timestamp.
   *
   * @param tenantId The internal tenant UUID.
   * @param gameId The game UUID identifier.
   * @return true if the active {@code Game} entity was found and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID gameId);

  @Override
  default Optional<Game> findById(UUID id) {
    throw new UnsupportedOperationException(
        "Direct game lookup requires tenant context; use findById(tenantId, gameId)");
  }

  @Override
  default ImmutableList<Game> list() {
    throw new UnsupportedOperationException(
        "Direct un-tenanted game listing is not permitted in user space; "
            + "use listAll(tenantId, systemId, campaignId)");
  }
}
