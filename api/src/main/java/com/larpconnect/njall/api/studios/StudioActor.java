package com.larpconnect.njall.api.studios;

import com.google.common.base.Strings;
import com.larpconnect.njall.data.dao.StudioDAO;
import com.larpconnect.njall.data.dao.StudioLookupDAO;
import com.larpconnect.njall.data.domain.DeletionFilter;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.Optional;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted studio operations. */
public final class StudioActor extends AbstractBehavior<StudioCommand> {

  private final Logger logger = LoggerFactory.getLogger(StudioActor.class);
  private final StudioLookupDAO studioLookupDao;
  private final StudioDAO studioDao;

  public StudioActor(
      ActorContext<StudioCommand> context, StudioLookupDAO studioLookupDao, StudioDAO studioDao) {
    super(context);
    this.studioLookupDao = studioLookupDao;
    this.studioDao = studioDao;
  }

  @Override
  public Receive<StudioCommand> createReceive() {
    return newReceiveBuilder().onMessage(StudioCommand.GetStudio.class, this::onGetStudio).build();
  }

  private Behavior<StudioCommand> onGetStudio(StudioCommand.GetStudio cmd) {
    try {
      processGetStudio(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get studio", e);
    }
    return this;
  }

  private void processGetStudio(StudioCommand.GetStudio cmd) {
    var maybeLookup = resolveLookup(cmd.studioIdParam());
    if (maybeLookup.isEmpty()) {
      replyNotFound(cmd.replyTo(), "Studio not found: " + cmd.studioIdParam());
      return;
    }
    fetchAndReplyStudio(maybeLookup.get(), cmd.replyTo());
  }

  private Optional<StudioLookup> resolveLookup(String studioIdParam) {
    var maybeUuid = StudioValidation.tryParseUuid(studioIdParam);
    if (maybeUuid.isPresent()) {
      return studioLookupDao.findById(maybeUuid.get(), DeletionFilter.ACTIVE_ONLY);
    }
    return studioLookupDao.findByAlias(studioIdParam, DeletionFilter.ACTIVE_ONLY);
  }

  private void fetchAndReplyStudio(StudioLookup lookup, ActorRef<StudioActorResponse> replyTo) {
    var maybeStudio = studioDao.findById(lookup.tenantId());
    if (maybeStudio.isPresent()) {
      var studio = maybeStudio.get();
      var response = new StudioResponse(lookup.studioId(), lookup.alias(), studio.name());
      replyTo.tell(StudioActorResponse.success(response));
    } else {
      replyTo.tell(
          StudioActorResponse.notFound("Studio not found for tenant: " + lookup.tenantId()));
    }
  }

  private void replyNotFound(ActorRef<StudioActorResponse> replyTo, String message) {
    replyTo.tell(StudioActorResponse.notFound(message));
  }

  private void handleError(
      ActorRef<StudioActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in StudioActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(StudioActorResponse.failure(reason));
  }
}
