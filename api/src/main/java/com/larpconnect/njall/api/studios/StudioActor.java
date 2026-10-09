package com.larpconnect.njall.api.studios;

import com.google.common.base.Strings;
import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.domain.Studio;
import com.larpconnect.njall.data.domain.StudioLookup;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted studio operations. */
final class StudioActor extends AbstractBehavior<StudioCommand> {

  private final Logger logger = LoggerFactory.getLogger(StudioActor.class);
  private final StudioDAO studioDao;

  StudioActor(ActorContext<StudioCommand> context, StudioDAO studioDao) {
    super(context);
    this.studioDao = studioDao;
  }

  @Override
  public Receive<StudioCommand> createReceive() {
    return newReceiveBuilder().onMessage(StudioCommand.GetStudio.class, this::onGetStudio).build();
  }

  private Behavior<StudioCommand> onGetStudio(StudioCommand.GetStudio cmd) {
    try {
      fetchAndReplyStudio(cmd.lookup(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get studio", e);
    }
    return this;
  }

  private void fetchAndReplyStudio(StudioLookup lookup, ActorRef<StudioActorResponse> replyTo) {
    studioDao
        .findById(lookup.tenantId())
        .ifPresentOrElse(
            studio ->
                replyTo.tell(StudioActorResponse.success(createStudioResponse(lookup, studio))),
            () ->
                replyTo.tell(
                    StudioActorResponse.notFound(
                        "Studio not found for tenant: " + lookup.tenantId())));
  }

  private StudioResponse createStudioResponse(StudioLookup lookup, Studio studio) {
    return new StudioResponse(lookup.studioId(), lookup.alias(), studio.name());
  }

  private void handleError(
      ActorRef<StudioActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in StudioActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(StudioActorResponse.failure(reason));
  }
}
