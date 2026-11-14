package com.larpconnect.njall.api.studios.games;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link GameActor}. */
public sealed interface GameCommand {

  /** Command to create a new game within a studio. */
  @Immutable
  record CreateGame(
      UUID tenantId,
      String studioIdentifier,
      Optional<StudioLookup> lookup,
      CreateGameRequest request,
      ActorRef<GameActorResponse> replyTo)
      implements GameCommand {

    /** Creates a command without an explicit studio lookup entry. */
    public CreateGame(
        UUID tenantId,
        String studioIdentifier,
        CreateGameRequest request,
        ActorRef<GameActorResponse> replyTo) {
      this(tenantId, studioIdentifier, Optional.empty(), request, replyTo);
    }
  }

  /** Command to retrieve a single game by identifier. */
  @Immutable
  record GetGame(
      UUID tenantId, String studioIdentifier, UUID gameId, ActorRef<GameActorResponse> replyTo)
      implements GameCommand {}

  /** Command to list active games within a studio with optional system/campaign filters. */
  @Immutable
  record ListGames(
      UUID tenantId,
      String studioIdentifier,
      Optional<StudioLookup> lookup,
      Optional<String> systemId,
      Optional<String> campaignId,
      ActorRef<GameActorResponse> replyTo)
      implements GameCommand {

    /** Creates a list command without an explicit studio lookup entry. */
    public ListGames(
        UUID tenantId,
        String studioIdentifier,
        Optional<String> systemId,
        Optional<String> campaignId,
        ActorRef<GameActorResponse> replyTo) {
      this(tenantId, studioIdentifier, Optional.empty(), systemId, campaignId, replyTo);
    }
  }

  /** Command to partially update a game within a studio. */
  @Immutable
  record PatchGame(
      UUID tenantId,
      String studioIdentifier,
      Optional<StudioLookup> lookup,
      UUID gameId,
      UpdateGameRequest request,
      Optional<String> updateMask,
      ActorRef<GameActorResponse> replyTo)
      implements GameCommand {

    /** Creates a patch command without an explicit studio lookup entry. */
    public PatchGame(
        UUID tenantId,
        String studioIdentifier,
        UUID gameId,
        UpdateGameRequest request,
        Optional<String> updateMask,
        ActorRef<GameActorResponse> replyTo) {
      this(tenantId, studioIdentifier, Optional.empty(), gameId, request, updateMask, replyTo);
    }
  }

  /** Command to soft-delete a game within a studio. */
  @Immutable
  record DeleteGame(UUID tenantId, UUID gameId, ActorRef<GameActorResponse> replyTo)
      implements GameCommand {}
}
