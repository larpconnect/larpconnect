package com.larpconnect.njall.api.studios.games;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import com.larpconnect.njall.api.studios.common.StudioResourceUriSupport;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.dao.studios.games.CreateGameParams;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import com.larpconnect.njall.data.dao.studios.games.PatchGameParams;
import com.larpconnect.njall.data.domain.studios.Game;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing tenanted studio game operations. */
final class GameActor {

  private final Logger logger = LoggerFactory.getLogger(GameActor.class);
  private final GameDAO gameDao;

  private GameActor(GameDAO gameDao) {
    this.gameDao = gameDao;
  }

  static Behavior<GameCommand> create(GameDAO gameDao) {
    return createActor(gameDao).behavior();
  }

  private static GameActor createActor(GameDAO gameDao) {
    return new GameActor(gameDao);
  }

  private Behavior<GameCommand> behavior() {
    return Behaviors.receiveMessage(this::onCommand);
  }

  private Behavior<GameCommand> onCommand(GameCommand command) {
    return switch (command) {
      case GameCommand.CreateGame cmd -> onCreateGame(cmd);
      case GameCommand.GetGame cmd -> onGetGame(cmd);
      case GameCommand.ListGames cmd -> onListGames(cmd);
      case GameCommand.PatchGame cmd -> onPatchGame(cmd);
      case GameCommand.DeleteGame cmd -> onDeleteGame(cmd);
    };
  }

  private Behavior<GameCommand> onCreateGame(GameCommand.CreateGame cmd) {
    try {
      var validationError = GameValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(GameActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      var created = gameDao.create(cmd.tenantId(), buildCreateParams(cmd));
      cmd.replyTo().tell(GameActorResponse.success(toResponse(cmd.studioIdentifier(), created)));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create game", e);
    }
    return Behaviors.same();
  }

  private Behavior<GameCommand> onGetGame(GameCommand.GetGame cmd) {
    try {
      gameDao
          .findById(cmd.tenantId(), cmd.gameId())
          .ifPresentOrElse(
              game ->
                  cmd.replyTo()
                      .tell(GameActorResponse.success(toResponse(cmd.studioIdentifier(), game))),
              () ->
                  cmd.replyTo()
                      .tell(GameActorResponse.notFound("Game not found: " + cmd.gameId())));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get game", e);
    }
    return Behaviors.same();
  }

  private Behavior<GameCommand> onListGames(GameCommand.ListGames cmd) {
    try {
      var systemId =
          StudioResourceUriSupport.parseOptionalResourceUri(
              cmd.systemId(),
              cmd.studioIdentifier(),
              StudioResourceUriSupport.SYSTEMS_COLLECTION,
              cmd.lookup());
      var campaignId =
          StudioResourceUriSupport.parseOptionalResourceUri(
              cmd.campaignId(),
              cmd.studioIdentifier(),
              StudioResourceUriSupport.CAMPAIGNS_COLLECTION,
              cmd.lookup());
      var games =
          gameDao.listAll(cmd.tenantId(), systemId, campaignId).stream()
              .map(game -> toResponse(cmd.studioIdentifier(), game))
              .collect(toImmutableList());
      cmd.replyTo().tell(GameActorResponse.items(games));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list games", e);
    }
    return Behaviors.same();
  }

  private Behavior<GameCommand> onPatchGame(GameCommand.PatchGame cmd) {
    try {
      var maskFields = UpdateMaskSupport.parseUpdateMask(cmd.updateMask());
      var validationError = GameValidation.validatePatch(cmd.request(), maskFields);
      if (validationError.isPresent()) {
        cmd.replyTo().tell(GameActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      gameDao
          .patch(cmd.tenantId(), cmd.gameId(), buildPatchParams(cmd, maskFields))
          .ifPresentOrElse(
              updated ->
                  cmd.replyTo()
                      .tell(GameActorResponse.success(toResponse(cmd.studioIdentifier(), updated))),
              () ->
                  cmd.replyTo()
                      .tell(GameActorResponse.notFound("Game not found: " + cmd.gameId())));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch game", e);
    }
    return Behaviors.same();
  }

  private Behavior<GameCommand> onDeleteGame(GameCommand.DeleteGame cmd) {
    try {
      if (gameDao.softDelete(cmd.tenantId(), cmd.gameId())) {
        cmd.replyTo().tell(GameActorResponse.deleted());
      } else {
        cmd.replyTo().tell(GameActorResponse.notFound("Game not found: " + cmd.gameId()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete game", e);
    }
    return Behaviors.same();
  }

  private static CreateGameParams buildCreateParams(GameCommand.CreateGame cmd) {
    var request = cmd.request();
    var generatorId =
        StudioResourceUriSupport.parseResourceUri(
            request.generator(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.ACTORS_COLLECTION,
            cmd.lookup());
    var systemId =
        StudioResourceUriSupport.parseResourceUri(
            request.system(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.SYSTEMS_COLLECTION,
            cmd.lookup());
    var campaignId =
        StudioResourceUriSupport.parseOptionalResourceUri(
            request.campaign(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.CAMPAIGNS_COLLECTION,
            cmd.lookup());
    return createCreateParams(generatorId, systemId, campaignId, request);
  }

  private static CreateGameParams createCreateParams(
      UUID generatorId, UUID systemId, Optional<UUID> campaignId, CreateGameRequest request) {
    return new CreateGameParams(
        generatorId,
        systemId,
        campaignId,
        request.title(),
        request.summary(),
        request.locationId(),
        request.startTime(),
        request.endTime());
  }

  @Immutable
  private record MaskedField<@ImmutableTypeParameter T>(Optional<T> value, boolean cleared) {}

  private static PatchGameParams buildPatchParams(
      GameCommand.PatchGame cmd, Optional<ImmutableSet<String>> maskFields) {
    var request = cmd.request();
    var systemId =
        parseTargetedUri(
            maskFields,
            request.system(),
            cmd,
            StudioResourceUriSupport.SYSTEMS_COLLECTION,
            "system");
    var campaignId =
        parseTargetedUri(
            maskFields,
            request.campaign(),
            cmd,
            StudioResourceUriSupport.CAMPAIGNS_COLLECTION,
            "campaign");
    var clearCampaign =
        UpdateMaskSupport.isFieldCleared(maskFields, request.campaign(), "campaign");
    var campaign = createMaskedField(campaignId, clearCampaign);
    var title =
        UpdateMaskSupport.selectOptionalField(
            request.title(), Optional.empty(), maskFields, "title");
    var summary = resolveMaskedField(maskFields, request.summary(), "summary");
    var locationId =
        resolveMaskedField(maskFields, request.locationId(), "locationId", "location_id");
    var startTime = resolveMaskedField(maskFields, request.startTime(), "startTime", "start_time");
    var endTime = resolveMaskedField(maskFields, request.endTime(), "endTime", "end_time");
    return createPatchParams(systemId, campaign, title, summary, locationId, startTime, endTime);
  }

  private static <T> MaskedField<T> resolveMaskedField(
      Optional<ImmutableSet<String>> maskFields, Optional<T> requestValue, String... fieldNames) {
    var value =
        UpdateMaskSupport.selectOptionalField(
            requestValue, Optional.empty(), maskFields, fieldNames);
    var cleared = UpdateMaskSupport.isFieldCleared(maskFields, requestValue, fieldNames);
    return createMaskedField(value, cleared);
  }

  private static <T> MaskedField<T> createMaskedField(Optional<T> value, boolean cleared) {
    return new MaskedField<>(value, cleared);
  }

  private static PatchGameParams createPatchParams(
      Optional<UUID> systemId,
      MaskedField<UUID> campaign,
      Optional<String> title,
      MaskedField<String> summary,
      MaskedField<UUID> locationId,
      MaskedField<Instant> startTime,
      MaskedField<Instant> endTime) {
    return new PatchGameParams(
        systemId,
        campaign.value(),
        campaign.cleared(),
        title,
        summary.value(),
        summary.cleared(),
        locationId.value(),
        locationId.cleared(),
        startTime.value(),
        startTime.cleared(),
        endTime.value(),
        endTime.cleared());
  }

  private static Optional<UUID> parseTargetedUri(
      Optional<ImmutableSet<String>> maskFields,
      Optional<String> rawValue,
      GameCommand.PatchGame cmd,
      String collection,
      String fieldName) {
    return UpdateMaskSupport.selectOptionalField(rawValue, Optional.empty(), maskFields, fieldName)
        .map(
            raw ->
                StudioResourceUriSupport.parseResourceUri(
                    raw, cmd.studioIdentifier(), collection, cmd.lookup()));
  }

  private static GameResponse toResponse(String studioIdentifier, Game game) {
    var generatorUri =
        StudioResourceUriSupport.formatResourceUri(
            studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, game.generator());
    var systemUri =
        StudioResourceUriSupport.formatResourceUri(
            studioIdentifier, StudioResourceUriSupport.SYSTEMS_COLLECTION, game.systemId());
    var campaignUri =
        game.campaignId()
            .map(
                id ->
                    StudioResourceUriSupport.formatResourceUri(
                        studioIdentifier, StudioResourceUriSupport.CAMPAIGNS_COLLECTION, id));
    return createGameResponse(game, generatorUri, systemUri, campaignUri);
  }

  private static GameResponse createGameResponse(
      Game game, String generatorUri, String systemUri, Optional<String> campaignUri) {
    return new GameResponse(
        game.id(),
        generatorUri,
        systemUri,
        campaignUri,
        game.eventType(),
        game.title(),
        game.summary(),
        game.locationId(),
        game.startTime(),
        game.endTime(),
        game.createdOn(),
        game.updatedOn());
  }

  private void handleError(ActorRef<GameActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in GameActor", operation, error);
    var message =
        Optional.ofNullable(error.getMessage())
            .filter(msg -> !msg.isBlank())
            .orElse("Error executing " + operation);
    if (error instanceof IllegalArgumentException) {
      replyTo.tell(GameActorResponse.badRequest(message));
      return;
    }
    replyTo.tell(GameActorResponse.failure(message));
  }
}
