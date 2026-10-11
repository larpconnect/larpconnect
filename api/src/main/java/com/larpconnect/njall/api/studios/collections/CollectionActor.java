package com.larpconnect.njall.api.studios.collections;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.Collection;
import com.larpconnect.njall.data.domain.ExpandedEntity;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted collection operations. */
final class CollectionActor extends AbstractBehavior<CollectionCommand> {

  private static final String TYPE_ORDERED_COLLECTION = "OrderedCollection";

  private final Logger logger = LoggerFactory.getLogger(CollectionActor.class);
  private final CollectionDAO collectionDao;

  CollectionActor(ActorContext<CollectionCommand> context, CollectionDAO collectionDao) {
    super(context);
    this.collectionDao = collectionDao;
  }

  @Override
  public Receive<CollectionCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(CollectionCommand.CreateCollection.class, this::onCreateCollection)
        .onMessage(CollectionCommand.GetCollection.class, this::onGetCollection)
        .onMessage(CollectionCommand.PatchCollection.class, this::onPatchCollection)
        .onMessage(CollectionCommand.DeleteCollection.class, this::onDeleteCollection)
        .onMessage(CollectionCommand.MutateItem.class, this::onMutateItem)
        .build();
  }

  private Behavior<CollectionCommand> onCreateCollection(CollectionCommand.CreateCollection cmd) {
    try {
      var validationError = CollectionValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(CollectionActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executeCreate(cmd.tenantId(), cmd.request(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create collection", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId, CreateCollectionRequest request, ActorRef<CollectionActorResponse> replyTo) {
    if (collectionDao.existsByOwnerAndName(
        tenantId, request.ownerId(), request.name(), Optional.empty())) {
      replyTo.tell(
          CollectionActorResponse.conflict(
              "Collection with name '" + request.name() + "' already exists for owner"));
      return;
    }
    var domain =
        collectionDao.create(tenantId, request.ownerId(), request.name(), request.summary());
    replyTo.tell(CollectionActorResponse.success(toCompressedResponse(domain)));
  }

  private Behavior<CollectionCommand> onGetCollection(CollectionCommand.GetCollection cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.collectionId(), cmd.form(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get collection", e);
    }
    return this;
  }

  private void executeGet(
      UUID tenantId,
      UUID collectionId,
      Optional<String> form,
      ActorRef<CollectionActorResponse> replyTo) {
    var collectionOpt = collectionDao.findById(tenantId, collectionId);
    if (collectionOpt.isEmpty()) {
      replyTo.tell(CollectionActorResponse.notFound("Collection not found: " + collectionId));
      return;
    }
    var collection = collectionOpt.orElseThrow();
    boolean isExpanded = form.filter("expanded"::equalsIgnoreCase).isPresent();
    if (isExpanded) {
      var expandedItems = collectionDao.findExpandedItems(tenantId, collectionId);
      replyTo.tell(CollectionActorResponse.success(toExpandedResponse(collection, expandedItems)));
    } else {
      replyTo.tell(CollectionActorResponse.success(toCompressedResponse(collection)));
    }
  }

  private Behavior<CollectionCommand> onPatchCollection(CollectionCommand.PatchCollection cmd) {
    try {
      var validationError = CollectionValidation.validatePatch(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(CollectionActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executePatch(
          cmd.tenantId(), cmd.collectionId(), cmd.request(), cmd.updateMask(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch collection", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      UUID collectionId,
      UpdateCollectionRequest request,
      Optional<String> updateMask,
      ActorRef<CollectionActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var name = selectField(maskFields, "name", request.name());
    var summary = selectField(maskFields, "summary", request.summary());
    persistPatch(tenantId, collectionId, name, summary, replyTo);
  }

  private void persistPatch(
      UUID tenantId,
      UUID collectionId,
      Optional<String> name,
      Optional<String> summary,
      ActorRef<CollectionActorResponse> replyTo) {
    if (name.isPresent()) {
      var existingOpt = collectionDao.findById(tenantId, collectionId);
      if (existingOpt.isEmpty()) {
        replyTo.tell(CollectionActorResponse.notFound("Collection not found: " + collectionId));
        return;
      }
      if (collectionDao.existsByOwnerAndName(
          tenantId,
          existingOpt.orElseThrow().ownerId(),
          name.orElseThrow(),
          Optional.of(collectionId))) {
        replyTo.tell(
            CollectionActorResponse.conflict(
                "Collection with name '" + name.orElseThrow() + "' already exists for owner"));
        return;
      }
    }

    collectionDao
        .patch(tenantId, collectionId, name, summary)
        .ifPresentOrElse(
            updated -> replyTo.tell(CollectionActorResponse.success(toCompressedResponse(updated))),
            () ->
                replyTo.tell(
                    CollectionActorResponse.notFound("Collection not found: " + collectionId)));
  }

  private Behavior<CollectionCommand> onDeleteCollection(CollectionCommand.DeleteCollection cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.collectionId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete collection", e);
    }
    return this;
  }

  private void executeDelete(
      UUID tenantId, UUID collectionId, ActorRef<CollectionActorResponse> replyTo) {
    boolean deleted = collectionDao.softDelete(tenantId, collectionId);
    if (deleted) {
      replyTo.tell(CollectionActorResponse.noContent());
    } else {
      replyTo.tell(CollectionActorResponse.notFound("Collection not found: " + collectionId));
    }
  }

  private Behavior<CollectionCommand> onMutateItem(CollectionCommand.MutateItem cmd) {
    try {
      if (cmd.action() == CollectionCommand.ItemAction.ADD) {
        executeAddItem(cmd);
      } else {
        executeRemoveItem(cmd);
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "mutate collection item", e);
    }
    return this;
  }

  private void executeAddItem(CollectionCommand.MutateItem cmd) {
    boolean added =
        collectionDao.addItem(
            cmd.tenantId(), cmd.collectionId(), cmd.refersToId(), cmd.before(), cmd.index());
    if (added) {
      cmd.replyTo().tell(CollectionActorResponse.noContent());
    } else {
      cmd.replyTo()
          .tell(CollectionActorResponse.notFound("Collection or referenced entity not found"));
    }
  }

  private void executeRemoveItem(CollectionCommand.MutateItem cmd) {
    boolean removed =
        collectionDao.removeItem(cmd.tenantId(), cmd.collectionId(), cmd.refersToId());
    if (removed) {
      cmd.replyTo().tell(CollectionActorResponse.noContent());
    } else {
      cmd.replyTo()
          .tell(CollectionActorResponse.notFound("Collection not found: " + cmd.collectionId()));
    }
  }

  private OrderedCollectionResponse toCompressedResponse(Collection collection) {
    return new OrderedCollectionResponse(
        collection.id(),
        TYPE_ORDERED_COLLECTION,
        collection.name(),
        collection.summary(),
        collection.ownerId(),
        collection.totalItems(),
        collection.itemIds(),
        collection.createdOn(),
        collection.updatedOn());
  }

  private OrderedCollectionResponse toExpandedResponse(
      Collection collection, ImmutableList<ExpandedEntity> expandedItems) {
    return new OrderedCollectionResponse(
        collection.id(),
        TYPE_ORDERED_COLLECTION,
        collection.name(),
        collection.summary(),
        collection.ownerId(),
        expandedItems.size(),
        expandedItems,
        collection.createdOn(),
        collection.updatedOn());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private <T> Optional<T> selectField(
      Optional<Set<String>> maskFields, String field, Optional<T> requestValue) {
    return maskFields
        .map(fields -> fields.contains(field) ? requestValue : Optional.<T>empty())
        .orElse(requestValue);
  }

  private void handleError(
      ActorRef<CollectionActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in CollectionActor", operation, error);
    if (isConstraintViolation(error)) {
      replyTo.tell(
          CollectionActorResponse.badRequest("Request data violates database constraints"));
      return;
    }
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(CollectionActorResponse.failure(reason));
  }

  static boolean isConstraintViolation(Throwable throwable) {
    var current = throwable;
    while (current != null) {
      if (current instanceof ConstraintViolationException) {
        return true;
      }
      var msg = current.getMessage();
      if (msg != null
          && (msg.contains("violates foreign key constraint")
              || msg.contains("violates unique constraint")
              || msg.contains("violates check constraint"))) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
