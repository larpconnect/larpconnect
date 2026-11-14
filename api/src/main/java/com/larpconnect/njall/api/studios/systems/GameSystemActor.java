package com.larpconnect.njall.api.studios.systems;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.api.studios.common.StudioResourceUriSupport;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import com.larpconnect.njall.data.dao.studios.systems.PatchGameSystemParams;
import com.larpconnect.njall.data.domain.studios.GameSystem;
import java.util.Optional;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted game system operations. */
final class GameSystemActor {

  private final Logger logger = LoggerFactory.getLogger(GameSystemActor.class);
  private final GameSystemDAO gameSystemDao;

  private GameSystemActor(GameSystemDAO gameSystemDao) {
    this.gameSystemDao = gameSystemDao;
  }

  static Behavior<GameSystemCommand> create(GameSystemDAO gameSystemDao) {
    return createActor(gameSystemDao).behavior();
  }

  private static GameSystemActor createActor(GameSystemDAO gameSystemDao) {
    return new GameSystemActor(gameSystemDao);
  }

  private Behavior<GameSystemCommand> behavior() {
    return Behaviors.receiveMessage(this::onCommand);
  }

  private Behavior<GameSystemCommand> onCommand(GameSystemCommand command) {
    return switch (command) {
      case GameSystemCommand.CreateGameSystem cmd -> onCreateGameSystem(cmd);
      case GameSystemCommand.GetGameSystem cmd -> onGetGameSystem(cmd);
      case GameSystemCommand.ListGameSystems cmd -> onListGameSystems(cmd);
      case GameSystemCommand.PatchGameSystem cmd -> onPatchGameSystem(cmd);
      case GameSystemCommand.DeleteGameSystem cmd -> onDeleteGameSystem(cmd);
    };
  }

  private Behavior<GameSystemCommand> onCreateGameSystem(GameSystemCommand.CreateGameSystem cmd) {
    try {
      var validationError = GameSystemValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(GameSystemActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      executeCreate(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create game system", e);
    }
    return Behaviors.same();
  }

  private void executeCreate(GameSystemCommand.CreateGameSystem cmd) {
    var request = cmd.request();
    var generatorId =
        StudioResourceUriSupport.parseResourceUri(
            request.generator(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.ACTORS_COLLECTION,
            cmd.lookup());
    var domain =
        gameSystemDao.create(cmd.tenantId(), generatorId, request.name(), request.summary());
    cmd.replyTo()
        .tell(GameSystemActorResponse.success(toSystemResponse(cmd.studioIdentifier(), domain)));
  }

  private Behavior<GameSystemCommand> onGetGameSystem(GameSystemCommand.GetGameSystem cmd) {
    try {
      executeGet(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get game system", e);
    }
    return Behaviors.same();
  }

  private void executeGet(GameSystemCommand.GetGameSystem cmd) {
    gameSystemDao
        .findById(cmd.tenantId(), cmd.systemId())
        .ifPresentOrElse(
            system ->
                cmd.replyTo()
                    .tell(
                        GameSystemActorResponse.success(
                            toSystemResponse(cmd.studioIdentifier(), system))),
            () ->
                cmd.replyTo()
                    .tell(
                        GameSystemActorResponse.notFound(
                            "Game system not found: " + cmd.systemId())));
  }

  private Behavior<GameSystemCommand> onListGameSystems(GameSystemCommand.ListGameSystems cmd) {
    try {
      executeList(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list game systems", e);
    }
    return Behaviors.same();
  }

  private void executeList(GameSystemCommand.ListGameSystems cmd) {
    var systems =
        gameSystemDao.listAll(cmd.tenantId()).stream()
            .map(system -> toSystemResponse(cmd.studioIdentifier(), system))
            .collect(toImmutableList());
    cmd.replyTo().tell(GameSystemActorResponse.items(systems));
  }

  private Behavior<GameSystemCommand> onPatchGameSystem(GameSystemCommand.PatchGameSystem cmd) {
    try {
      var maskFields = UpdateMaskSupport.parseUpdateMask(cmd.updateMask());
      var validationError = GameSystemValidation.validatePatch(cmd.request(), maskFields);
      if (validationError.isPresent()) {
        cmd.replyTo().tell(GameSystemActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      executePatch(cmd, maskFields);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch game system", e);
    }
    return Behaviors.same();
  }

  private void executePatch(
      GameSystemCommand.PatchGameSystem cmd, Optional<ImmutableSet<String>> maskFields) {
    var params = buildPatchParams(cmd.request(), maskFields);
    gameSystemDao
        .patch(cmd.tenantId(), cmd.systemId(), params)
        .ifPresentOrElse(
            updated ->
                cmd.replyTo()
                    .tell(
                        GameSystemActorResponse.success(
                            toSystemResponse(cmd.studioIdentifier(), updated))),
            () ->
                cmd.replyTo()
                    .tell(
                        GameSystemActorResponse.notFound(
                            "Game system not found: " + cmd.systemId())));
  }

  private Behavior<GameSystemCommand> onDeleteGameSystem(GameSystemCommand.DeleteGameSystem cmd) {
    try {
      executeDelete(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete game system", e);
    }
    return Behaviors.same();
  }

  private void executeDelete(GameSystemCommand.DeleteGameSystem cmd) {
    var deleted = gameSystemDao.softDelete(cmd.tenantId(), cmd.systemId());
    if (deleted) {
      cmd.replyTo().tell(GameSystemActorResponse.deleted());
    } else {
      cmd.replyTo()
          .tell(GameSystemActorResponse.notFound("Game system not found: " + cmd.systemId()));
    }
  }

  private static PatchGameSystemParams buildPatchParams(
      UpdateGameSystemRequest request, Optional<ImmutableSet<String>> maskFields) {
    var name =
        UpdateMaskSupport.selectOptionalField(request.name(), Optional.empty(), maskFields, "name");
    var summary =
        UpdateMaskSupport.selectOptionalField(
            request.summary(), Optional.empty(), maskFields, "summary");
    var clearSummary = UpdateMaskSupport.isFieldCleared(maskFields, request.summary(), "summary");
    return createPatchParams(name, summary, clearSummary);
  }

  private static PatchGameSystemParams createPatchParams(
      Optional<String> name, Optional<String> summary, boolean clearSummary) {
    return new PatchGameSystemParams(name, summary, clearSummary);
  }

  private static GameSystemResponse toSystemResponse(String studioIdentifier, GameSystem system) {
    var generatorUri =
        StudioResourceUriSupport.formatResourceUri(
            studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, system.generator());
    return createSystemResponse(system, generatorUri);
  }

  private static GameSystemResponse createSystemResponse(GameSystem system, String generatorUri) {
    return new GameSystemResponse(
        system.id(),
        generatorUri,
        system.name(),
        system.summary(),
        system.createdOn(),
        system.updatedOn());
  }

  private void handleError(
      ActorRef<GameSystemActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in GameSystemActor", operation, error);
    if (error instanceof IllegalArgumentException) {
      replyTo.tell(
          GameSystemActorResponse.badRequest(resolveMessage(error, "Invalid game system request")));
      return;
    }
    if (error instanceof IllegalStateException || isConstraintViolation(error)) {
      replyTo.tell(
          GameSystemActorResponse.conflict(
              resolveMessage(error, "Game system name already exists")));
      return;
    }
    replyTo.tell(
        GameSystemActorResponse.failure(resolveMessage(error, "Error executing " + operation)));
  }

  private boolean isConstraintViolation(@Nullable Throwable error) {
    var current = error;
    while (current != null) {
      if (current instanceof ConstraintViolationException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private String resolveMessage(Throwable error, String fallback) {
    var message = error.getMessage();
    return !Strings.isNullOrEmpty(message) ? message : fallback;
  }
}
