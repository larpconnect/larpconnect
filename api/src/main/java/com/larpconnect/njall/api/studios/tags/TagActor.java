package com.larpconnect.njall.api.studios.tags;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import com.larpconnect.njall.data.domain.Hashtag;
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

/** Apache Pekko Typed actor executing user-space tenanted hashtag operations. */
final class TagActor extends AbstractBehavior<TagCommand> {

  private final Logger logger = LoggerFactory.getLogger(TagActor.class);
  private final HashtagDAO hashtagDao;

  TagActor(ActorContext<TagCommand> context, HashtagDAO hashtagDao) {
    super(context);
    this.hashtagDao = hashtagDao;
  }

  @Override
  public Receive<TagCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(TagCommand.CreateTags.class, this::onCreateTags)
        .onMessage(TagCommand.QueryTag.class, this::onQueryTag)
        .onMessage(TagCommand.PatchTag.class, this::onPatchTag)
        .onMessage(TagCommand.DeleteTag.class, this::onDeleteTag)
        .build();
  }

  private Behavior<TagCommand> onCreateTags(TagCommand.CreateTags cmd) {
    try {
      if (cmd.batch()) {
        var batchRequest = new BatchCreateTagsRequest(cmd.requests());
        var validationError = TagValidation.validateBatchCreate(batchRequest);
        if (validationError.isPresent()) {
          cmd.replyTo().tell(TagActorResponse.badRequest(validationError.orElseThrow()));
          return this;
        }
        executeBatchCreate(cmd.tenantId(), cmd.canonicalPrefix(), cmd.requests(), cmd.replyTo());
      } else {
        var request = cmd.requests().getFirst();
        var validationError = TagValidation.validateCreate(request);
        if (validationError.isPresent()) {
          cmd.replyTo().tell(TagActorResponse.badRequest(validationError.orElseThrow()));
          return this;
        }
        executeCreate(cmd.tenantId(), cmd.canonicalPrefix(), request, cmd.replyTo());
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create tag", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId,
      String canonicalPrefix,
      CreateTagRequest request,
      ActorRef<TagActorResponse> replyTo) {
    var sanitizedTag = TagValidation.sanitizeTag(request.tag());
    var existingOpt = hashtagDao.findByTag(tenantId, sanitizedTag);
    if (existingOpt.isPresent()) {
      replyTo.tell(TagActorResponse.success(toTagResponse(existingOpt.orElseThrow()), false));
      return;
    }
    var canonicalUrl = canonicalPrefix + "/" + sanitizedTag;
    var created = hashtagDao.create(tenantId, sanitizedTag, canonicalUrl, request.summary());
    replyTo.tell(TagActorResponse.success(toTagResponse(created), true));
  }

  private void executeBatchCreate(
      UUID tenantId,
      String canonicalPrefix,
      ImmutableList<CreateTagRequest> requests,
      ActorRef<TagActorResponse> replyTo) {
    var items =
        requests.stream()
            .map(
                req -> {
                  var sanitized = TagValidation.sanitizeTag(req.tag());
                  var canonicalUrl = canonicalPrefix + "/" + sanitized;
                  return new HashtagDAO.TagCreationItem(sanitized, canonicalUrl, req.summary());
                })
            .toList();
    var created = hashtagDao.batchCreate(tenantId, items);
    var responses =
        created.stream().map(this::toTagResponse).collect(ImmutableList.toImmutableList());
    replyTo.tell(TagActorResponse.items(responses));
  }

  private Behavior<TagCommand> onQueryTag(TagCommand.QueryTag cmd) {
    try {
      if (cmd.idOrName().isPresent()) {
        executeGet(cmd.tenantId(), cmd.idOrName().orElseThrow(), cmd.replyTo());
      } else {
        executeList(cmd.tenantId(), cmd.replyTo());
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "query tag", e);
    }
    return this;
  }

  private void executeGet(UUID tenantId, String idOrName, ActorRef<TagActorResponse> replyTo) {
    var uuidOpt = TagValidation.tryParseUuid(idOrName);
    var tagOpt =
        uuidOpt.isPresent()
            ? hashtagDao.findById(tenantId, uuidOpt.orElseThrow())
            : hashtagDao.findByTag(tenantId, TagValidation.sanitizeTag(idOrName));
    tagOpt.ifPresentOrElse(
        tag -> replyToSuccess(replyTo, tag),
        () -> replyToNotFound(replyTo, "Hashtag not found: " + idOrName));
  }

  private void executeList(UUID tenantId, ActorRef<TagActorResponse> replyTo) {
    var tags = hashtagDao.listAll(tenantId);
    var responses = tags.stream().map(this::toTagResponse).collect(ImmutableList.toImmutableList());
    replyTo.tell(TagActorResponse.items(responses));
  }

  private Behavior<TagCommand> onPatchTag(TagCommand.PatchTag cmd) {
    try {
      var validationError = TagValidation.validateUpdate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(TagActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      var targetIdOpt = resolveTagId(cmd.tenantId(), cmd.idOrName());
      if (targetIdOpt.isEmpty()) {
        replyToNotFound(cmd.replyTo(), "Hashtag not found: " + cmd.idOrName());
        return this;
      }
      executePatch(
          cmd.tenantId(),
          targetIdOpt.orElseThrow(),
          cmd.request(),
          cmd.updateMask(),
          cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch tag", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      UUID tagId,
      UpdateTagRequest request,
      Optional<String> updateMask,
      ActorRef<TagActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var tag = selectField(maskFields, "tag", request.tag()).map(TagValidation::sanitizeTag);
    var summary = selectField(maskFields, "summary", request.summary());

    hashtagDao
        .patch(tenantId, tagId, tag, summary)
        .ifPresentOrElse(
            updated -> replyTo.tell(TagActorResponse.success(toTagResponse(updated), false)),
            () -> replyTo.tell(TagActorResponse.notFound("Hashtag not found: " + tagId)));
  }

  private Behavior<TagCommand> onDeleteTag(TagCommand.DeleteTag cmd) {
    try {
      var targetIdOpt = resolveTagId(cmd.tenantId(), cmd.idOrName());
      if (targetIdOpt.isEmpty()) {
        replyToNotFound(cmd.replyTo(), "Hashtag not found: " + cmd.idOrName());
        return this;
      }
      executeDelete(cmd.tenantId(), targetIdOpt.orElseThrow(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete tag", e);
    }
    return this;
  }

  private void executeDelete(UUID tenantId, UUID tagId, ActorRef<TagActorResponse> replyTo) {
    var deleted = hashtagDao.softDelete(tenantId, tagId);
    if (deleted) {
      replyTo.tell(TagActorResponse.deleted());
    } else {
      replyTo.tell(TagActorResponse.notFound("Hashtag not found: " + tagId));
    }
  }

  private Optional<UUID> resolveTagId(UUID tenantId, String idOrName) {
    var uuidOpt = TagValidation.tryParseUuid(idOrName);
    if (uuidOpt.isPresent()) {
      return uuidOpt;
    }
    return hashtagDao.findByTag(tenantId, TagValidation.sanitizeTag(idOrName)).map(Hashtag::id);
  }

  private void replyToSuccess(ActorRef<TagActorResponse> replyTo, Hashtag tag) {
    replyTo.tell(TagActorResponse.success(toTagResponse(tag), false));
  }

  private void replyToNotFound(ActorRef<TagActorResponse> replyTo, String message) {
    replyTo.tell(TagActorResponse.notFound(message));
  }

  private TagResponse toTagResponse(Hashtag tag) {
    return new TagResponse(
        tag.id(),
        tag.tag(),
        tag.linkType(),
        tag.url(),
        tag.mediaType(),
        tag.summary(),
        tag.createdOn(),
        tag.updatedOn());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private Optional<String> selectField(
      Optional<Set<String>> maskFields, String fieldName, Optional<String> requestValue) {
    return maskFields
        .map(set -> set.contains(fieldName) ? requestValue : Optional.<String>empty())
        .orElse(requestValue);
  }

  private void handleError(ActorRef<TagActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in TagActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(TagActorResponse.failure(reason));
  }
}
