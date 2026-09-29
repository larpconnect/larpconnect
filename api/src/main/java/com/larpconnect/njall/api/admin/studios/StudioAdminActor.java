package com.larpconnect.njall.api.admin.studios;

import com.google.common.base.Strings;
import com.larpconnect.njall.api.admin.common.AdminValidation;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.StudioLookupDAO;
import com.larpconnect.njall.data.domain.DeletionFilter;
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
  private final StudioLookupDAO studioLookupDao;
  private final StudioLookupCache studioLookupCache;

  StudioAdminActor(
      ActorContext<StudioAdminCommand> context,
      StudioLookupDAO studioLookupDao,
      StudioLookupCache studioLookupCache) {
    super(context);
    this.studioLookupDao = studioLookupDao;
    this.studioLookupCache = studioLookupCache;
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
      executeCreateStudio(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create studio", e);
    }
    return this;
  }

  private void executeCreateStudio(StudioAdminCommand.CreateStudio cmd) {
    if (rejectIfInvalidAlias(cmd.alias(), cmd.replyTo())) {
      return;
    }
    if (rejectIfDuplicateAlias(cmd.alias(), cmd.replyTo())) {
      return;
    }
    createAndReplyStudio(cmd.alias(), resolveStudioName(cmd), cmd.replyTo());
  }

  private boolean rejectIfInvalidAlias(String alias, ActorRef<StudioAdminResponse> replyTo) {
    if (!AdminValidation.isValidIdentifier(alias)) {
      replyTo.tell(StudioAdminResponse.badRequest("Invalid studio alias: " + alias));
      return true;
    }
    return false;
  }

  private boolean rejectIfDuplicateAlias(String alias, ActorRef<StudioAdminResponse> replyTo) {
    var existing = studioLookupDao.findByAlias(alias, DeletionFilter.INCLUDE_DELETED);
    if (existing.isPresent()) {
      replyTo.tell(StudioAdminResponse.conflict("Studio alias already exists: " + alias));
      return true;
    }
    return false;
  }

  private String resolveStudioName(StudioAdminCommand.CreateStudio cmd) {
    return cmd.name().filter(n -> !n.isBlank()).orElse(cmd.alias());
  }

  private void createAndReplyStudio(
      String alias, String name, ActorRef<StudioAdminResponse> replyTo) {
    var studio = studioLookupDao.create(alias, name);
    studioLookupCache.refresh();
    replyTo.tell(StudioAdminResponse.single(studio));
  }

  private Behavior<StudioAdminCommand> onListStudios(StudioAdminCommand.ListStudios cmd) {
    try {
      var studios = studioLookupDao.list(cmd.filter());
      cmd.replyTo().tell(StudioAdminResponse.list(studios));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list studios", e);
    }
    return this;
  }

  private Behavior<StudioAdminCommand> onGetStudioById(StudioAdminCommand.GetStudioById cmd) {
    try {
      studioLookupDao
          .findById(cmd.studioId(), cmd.filter())
          .ifPresentOrElse(
              studio -> cmd.replyTo().tell(StudioAdminResponse.single(studio)),
              () ->
                  cmd.replyTo()
                      .tell(StudioAdminResponse.notFound("Studio not found: " + cmd.studioId())));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get studio by id", e);
    }
    return this;
  }

  private Behavior<StudioAdminCommand> onGetStudioByAlias(StudioAdminCommand.GetStudioByAlias cmd) {
    try {
      studioLookupDao
          .findByAlias(cmd.alias(), cmd.filter())
          .ifPresentOrElse(
              studio -> cmd.replyTo().tell(StudioAdminResponse.single(studio)),
              () ->
                  cmd.replyTo()
                      .tell(StudioAdminResponse.notFound("Studio not found: " + cmd.alias())));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get studio by alias", e);
    }
    return this;
  }

  private void handleError(
      ActorRef<StudioAdminResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in StudioAdminActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(StudioAdminResponse.failure(reason));
  }
}
