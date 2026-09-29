package com.larpconnect.njall.api.studios;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.dao.LinkDAO;
import com.larpconnect.njall.data.domain.Link;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted link operations. */
public final class LinkActor extends AbstractBehavior<LinkCommand> {

  private static final String DEFAULT_MEDIA_TYPE = "text/html";
  private final Logger logger = LoggerFactory.getLogger(LinkActor.class);
  private final LinkDAO linkDao;

  LinkActor(ActorContext<LinkCommand> context, LinkDAO linkDao) {
    super(context);
    this.linkDao = linkDao;
  }

  @Override
  public Receive<LinkCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(LinkCommand.CreateLink.class, this::onCreateLink)
        .onMessage(LinkCommand.GetLink.class, this::onGetLink)
        .onMessage(LinkCommand.PatchLink.class, this::onPatchLink)
        .onMessage(LinkCommand.DeleteLink.class, this::onDeleteLink)
        .build();
  }

  private Behavior<LinkCommand> onCreateLink(LinkCommand.CreateLink cmd) {
    try {
      var validationError = LinkValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(LinkActorResponse.badRequest(validationError.get()));
        return this;
      }
      executeCreate(cmd.tenantId(), cmd.request(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create link", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId, CreateLinkRequest request, ActorRef<LinkActorResponse> replyTo) {
    var mediaType = request.mediaType().filter(s -> !s.isBlank()).orElse(DEFAULT_MEDIA_TYPE);
    var domain =
        linkDao.create(tenantId, request.linkType(), request.url(), mediaType, request.summary());
    replyTo.tell(LinkActorResponse.success(toLinkResponse(domain)));
  }

  private Behavior<LinkCommand> onGetLink(LinkCommand.GetLink cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.linkId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get link", e);
    }
    return this;
  }

  private void executeGet(UUID tenantId, UUID linkId, ActorRef<LinkActorResponse> replyTo) {
    linkDao
        .findById(tenantId, linkId)
        .ifPresentOrElse(
            link -> replyTo.tell(LinkActorResponse.success(toLinkResponse(link))),
            () -> replyTo.tell(LinkActorResponse.notFound("Link not found: " + linkId)));
  }

  private Behavior<LinkCommand> onPatchLink(LinkCommand.PatchLink cmd) {
    try {
      var validationError = LinkValidation.validateUpdate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(LinkActorResponse.badRequest(validationError.get()));
        return this;
      }
      executePatch(cmd.tenantId(), cmd.linkId(), cmd.request(), cmd.updateMask(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch link", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      UUID linkId,
      UpdateLinkRequest request,
      Optional<String> updateMask,
      ActorRef<LinkActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var linkType = selectField(maskFields, "linkType", request.linkType());
    var url = selectField(maskFields, "url", request.url());
    var mediaType = selectField(maskFields, "mediaType", request.mediaType());
    var summary = selectField(maskFields, "summary", request.summary());

    linkDao
        .patch(tenantId, linkId, linkType, url, mediaType, summary)
        .ifPresentOrElse(
            updated -> replyTo.tell(LinkActorResponse.success(toLinkResponse(updated))),
            () -> replyTo.tell(LinkActorResponse.notFound("Link not found: " + linkId)));
  }

  private LinkResponse toLinkResponse(Link link) {
    return new LinkResponse(
        link.id(),
        link.linkType(),
        link.url(),
        link.mediaType(),
        link.summary(),
        link.createdOn(),
        link.updatedOn());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private Optional<String> selectField(
      Optional<Set<String>> maskFields, String fieldName, Optional<String> requestValue) {
    if (maskFields.isPresent()) {
      return maskFields.get().contains(fieldName) ? requestValue : Optional.empty();
    }
    return requestValue;
  }

  private Behavior<LinkCommand> onDeleteLink(LinkCommand.DeleteLink cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.linkId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete link", e);
    }
    return this;
  }

  private void executeDelete(UUID tenantId, UUID linkId, ActorRef<LinkActorResponse> replyTo) {
    var deleted = linkDao.softDelete(tenantId, linkId);
    if (deleted) {
      replyTo.tell(LinkActorResponse.deleted());
    } else {
      replyTo.tell(LinkActorResponse.notFound("Link not found: " + linkId));
    }
  }

  private void handleError(ActorRef<LinkActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in LinkActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(LinkActorResponse.failure(reason));
  }
}
