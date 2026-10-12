package com.larpconnect.njall.data.dao.studios.actors;

import com.larpconnect.njall.data.domain.Agent;
import java.util.Optional;
import java.util.UUID;

/** DAO interface for tenanted studio actors (agents) and federated endpoints. */
public interface AgentDAO {

  /**
   * Retrieves an actor by tenant and actor UUID.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @return Optional containing the actor if found and active.
   */
  Optional<Agent> findById(UUID tenantId, UUID actorId);

  /**
   * Checks if an actor with the given preferred username already exists in the tenant.
   *
   * @param tenantId Studio tenant UUID.
   * @param preferredUsername Preferred username to check.
   * @param excludeActorId Optional actor UUID to exclude (e.g. for PATCH).
   * @return True if a conflicting actor exists.
   */
  boolean existsByPreferredUsername(
      UUID tenantId, String preferredUsername, Optional<UUID> excludeActorId);

  /**
   * Creates a new actor and provisions paired inbox and outbox collections.
   *
   * @param tenantId Studio tenant UUID.
   * @param ownerId Owner entity UUID.
   * @param preferredUsername Preferred username.
   * @param summary Optional description summary.
   * @return The created Agent domain record.
   */
  Agent create(UUID tenantId, UUID ownerId, String preferredUsername, Optional<String> summary);

  /**
   * Applies partial updates to an actor.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @param preferredUsername Optional new preferred username.
   * @param summary Optional new summary.
   * @return Optional containing the updated Agent if found and active.
   */
  Optional<Agent> update(
      UUID tenantId, UUID actorId, Optional<String> preferredUsername, Optional<String> summary);

  /**
   * Soft-deletes an actor and cascades soft deletion to its inbox and outbox collections.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @return True if the actor existed and was deleted, false otherwise.
   */
  boolean delete(UUID tenantId, UUID actorId);

  /**
   * Upserts a federated protocol endpoint for the specified actor.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @param name Endpoint name (alphanumeric, length < 32).
   * @param endpoint Target URI endpoint.
   * @return True if successfully added or overwritten, false if actor not found.
   */
  boolean addEndpoint(UUID tenantId, UUID actorId, String name, String endpoint);

  /**
   * Removes a federated protocol endpoint from the specified actor.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @param name Endpoint name.
   * @return True if successfully removed or idempotent, false if actor not found.
   */
  boolean removeEndpoint(UUID tenantId, UUID actorId, String name);

  /**
   * Retrieves the paired inbox collection UUID for an actor.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @return Optional containing inbox collection UUID if actor is active.
   */
  Optional<UUID> findInboxId(UUID tenantId, UUID actorId);

  /**
   * Retrieves the paired outbox collection UUID for an actor.
   *
   * @param tenantId Studio tenant UUID.
   * @param actorId Actor UUID.
   * @return Optional containing outbox collection UUID if actor is active.
   */
  Optional<UUID> findOutboxId(UUID tenantId, UUID actorId);
}
