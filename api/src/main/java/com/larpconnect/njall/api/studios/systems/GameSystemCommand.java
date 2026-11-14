package com.larpconnect.njall.api.studios.systems;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link GameSystemActor}. */
public sealed interface GameSystemCommand {

  /** Creates a new game system within a studio tenant. */
  @Immutable
  record CreateGameSystem(
      UUID tenantId,
      String studioIdentifier,
      CreateGameSystemRequest request,
      ActorRef<GameSystemActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements GameSystemCommand {

    /** Convenience constructor without a pre-resolved studio lookup. */
    public CreateGameSystem(
        UUID tenantId,
        String studioIdentifier,
        CreateGameSystemRequest request,
        ActorRef<GameSystemActorResponse> replyTo) {
      this(tenantId, studioIdentifier, request, replyTo, Optional.empty());
    }
  }

  /** Fetches a single game system by identifier within a studio tenant. */
  @Immutable
  record GetGameSystem(
      UUID tenantId,
      String studioIdentifier,
      UUID systemId,
      ActorRef<GameSystemActorResponse> replyTo)
      implements GameSystemCommand {}

  /** Lists all non-deleted game systems within a studio tenant. */
  @Immutable
  record ListGameSystems(
      UUID tenantId, String studioIdentifier, ActorRef<GameSystemActorResponse> replyTo)
      implements GameSystemCommand {}

  /** Partially updates an existing game system within a studio tenant. */
  @Immutable
  record PatchGameSystem(
      UUID tenantId,
      String studioIdentifier,
      UUID systemId,
      UpdateGameSystemRequest request,
      Optional<String> updateMask,
      ActorRef<GameSystemActorResponse> replyTo)
      implements GameSystemCommand {}

  /** Soft-deletes a game system within a studio tenant. */
  @Immutable
  record DeleteGameSystem(UUID tenantId, UUID systemId, ActorRef<GameSystemActorResponse> replyTo)
      implements GameSystemCommand {}
}
