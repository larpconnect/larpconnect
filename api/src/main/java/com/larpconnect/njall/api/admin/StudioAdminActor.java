package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.data.dao.StudioDAO;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing studio administration and lifecycle management operations. */
public final class StudioAdminActor extends AbstractBehavior<StudioAdminCommand> {

  private final Logger logger = LoggerFactory.getLogger(StudioAdminActor.class);
  private final StudioDAO studioDao;

  public StudioAdminActor(ActorContext<StudioAdminCommand> context, StudioDAO studioDao) {
    super(context);
    this.studioDao = studioDao;
  }

  @Override
  public Receive<StudioAdminCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(StudioAdminCommand.CreateStudio.class, this::onCreateStudio)
        .onMessage(StudioAdminCommand.ListStudios.class, this::onListStudios)
        .onMessage(StudioAdminCommand.GetStudioById.class, this::onGetStudioById)
        .onMessage(StudioAdminCommand.GetStudioByAlias.class, this::onGetStudioByAlias)
        .build();
  }

  private Behavior<StudioAdminCommand> onCreateStudio(StudioAdminCommand.CreateStudio cmd) {
    try {
      if (!AdminValidation.isValidIdentifier(cmd.alias())) {
        cmd.replyTo().tell(StudioAdminResponse.badRequest("Invalid studio alias: " + cmd.alias()));
        return this;
      }
      var existing = studioDao.findByAlias(cmd.alias(), true);
      if (existing.isPresent()) {
        cmd.replyTo()
            .tell(StudioAdminResponse.conflict("Studio alias already exists: " + cmd.alias()));
        return this;
      }
      var studio = studioDao.create(cmd.alias());
      cmd.replyTo().tell(StudioAdminResponse.single(studio));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create studio", e);
    }
    return this;
  }

  private Behavior<StudioAdminCommand> onListStudios(StudioAdminCommand.ListStudios cmd) {
    try {
      var studios = studioDao.list(cmd.includeDeleted());
      cmd.replyTo().tell(StudioAdminResponse.list(studios));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list studios", e);
    }
    return this;
  }

  private Behavior<StudioAdminCommand> onGetStudioById(StudioAdminCommand.GetStudioById cmd) {
    try {
      var studio = studioDao.findById(cmd.studioId(), cmd.includeDeleted());
      if (studio.isPresent()) {
        cmd.replyTo().tell(StudioAdminResponse.single(studio.get()));
      } else {
        cmd.replyTo().tell(StudioAdminResponse.notFound("Studio not found: " + cmd.studioId()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get studio by id", e);
    }
    return this;
  }

  private Behavior<StudioAdminCommand> onGetStudioByAlias(StudioAdminCommand.GetStudioByAlias cmd) {
    try {
      var studio = studioDao.findByAlias(cmd.alias(), cmd.includeDeleted());
      if (studio.isPresent()) {
        cmd.replyTo().tell(StudioAdminResponse.single(studio.get()));
      } else {
        cmd.replyTo().tell(StudioAdminResponse.notFound("Studio not found: " + cmd.alias()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get studio by alias", e);
    }
    return this;
  }

  private void handleError(
      ActorRef<StudioAdminResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in StudioAdminActor", operation, error);
    var message = error.getMessage();
    var reason = message != null && !message.isBlank() ? message : "Error executing " + operation;
    replyTo.tell(StudioAdminResponse.failure(reason));
  }
}
