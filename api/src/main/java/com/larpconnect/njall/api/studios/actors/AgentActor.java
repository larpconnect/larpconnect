package com.larpconnect.njall.api.studios.actors;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.api.studios.collections.OrderedCollectionResponse;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.Agent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted actor operations. */
final class AgentActor extends AbstractBehavior<AgentCommand> {

  private static final String TYPE_ACTOR = "Actor";
  private static final String TYPE_ORDERED_COLLECTION = "OrderedCollection";

  private final Logger logger = LoggerFactory.getLogger(AgentActor.class);
  private final AgentDAO agentDao;
  private final CollectionDAO collectionDao;

  AgentActor(ActorContext<AgentCommand> context, AgentDAO agentDao, CollectionDAO collectionDao) {
    super(context);
    this.agentDao = agentDao;
    this.collectionDao = collectionDao;
  }

  @Override
  public Receive<AgentCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(AgentCommand.CreateAgent.class, this::onCreateAgent)
        .onMessage(AgentCommand.GetAgent.class, this::onGetAgent)
        .onMessage(AgentCommand.PatchAgent.class, this::onPatchAgent)
        .onMessage(AgentCommand.DeleteAgent.class, this::onDeleteAgent)
        .onMessage(AgentCommand.MutateEndpoint.class, this::onMutateEndpoint)
        .onMessage(AgentCommand.AccessCollection.class, this::onAccessCollection)
        .build();
  }

  private Behavior<AgentCommand> onCreateAgent(AgentCommand.CreateAgent cmd) {
    try {
      var validationError = AgentValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(AgentActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      if (agentDao.existsByPreferredUsername(
          cmd.tenantId(), cmd.request().preferredUsername(), Optional.empty())) {
        cmd.replyTo()
            .tell(
                AgentActorResponse.conflict(
                    "Actor with preferred username '"
                        + cmd.request().preferredUsername()
                        + "' already exists in tenant"));
        return this;
      }
      var domain =
          agentDao.create(
              cmd.tenantId(),
              cmd.request().ownerId(),
              cmd.request().preferredUsername(),
              cmd.request().summary());
      cmd.replyTo()
          .tell(AgentActorResponse.success(toActorResponse(cmd.studioIdentifier(), domain)));
    } catch (IllegalArgumentException e) {
      cmd.replyTo().tell(AgentActorResponse.badRequest(e.getMessage()));
    } catch (IllegalStateException e) {
      cmd.replyTo().tell(AgentActorResponse.conflict(e.getMessage()));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create actor", e);
    }
    return this;
  }

  private Behavior<AgentCommand> onGetAgent(AgentCommand.GetAgent cmd) {
    try {
      agentDao
          .findById(cmd.tenantId(), cmd.actorId())
          .ifPresentOrElse(
              agent ->
                  cmd.replyTo()
                      .tell(
                          AgentActorResponse.success(
                              toActorResponse(cmd.studioIdentifier(), agent))),
              () ->
                  cmd.replyTo()
                      .tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId())));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get actor", e);
    }
    return this;
  }

  private Behavior<AgentCommand> onPatchAgent(AgentCommand.PatchAgent cmd) {
    try {
      var validationError = AgentValidation.validatePatch(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(AgentActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      var existingOpt = agentDao.findById(cmd.tenantId(), cmd.actorId());
      if (existingOpt.isEmpty()) {
        cmd.replyTo().tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId()));
        return this;
      }
      executePatch(cmd);
    } catch (IllegalArgumentException e) {
      cmd.replyTo().tell(AgentActorResponse.badRequest(e.getMessage()));
    } catch (IllegalStateException e) {
      cmd.replyTo().tell(AgentActorResponse.conflict(e.getMessage()));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch actor", e);
    }
    return this;
  }

  private void executePatch(AgentCommand.PatchAgent cmd) {
    var maskFields = parseUpdateMask(cmd.updateMask());
    var preferredUsername =
        selectField(maskFields, "preferredUsername", cmd.request().preferredUsername());
    var summary = selectField(maskFields, "summary", cmd.request().summary());

    if (preferredUsername.isPresent()) {
      var newUsername = preferredUsername.orElseThrow();
      if (newUsername.isBlank()) {
        cmd.replyTo().tell(AgentActorResponse.badRequest("Preferred username must not be blank"));
        return;
      }
      if (agentDao.existsByPreferredUsername(
          cmd.tenantId(), newUsername, Optional.of(cmd.actorId()))) {
        cmd.replyTo()
            .tell(AgentActorResponse.conflict("Preferred username already taken: " + newUsername));
        return;
      }
    }

    agentDao
        .update(cmd.tenantId(), cmd.actorId(), preferredUsername, summary)
        .ifPresentOrElse(
            updated ->
                cmd.replyTo()
                    .tell(
                        AgentActorResponse.success(
                            toActorResponse(cmd.studioIdentifier(), updated))),
            () ->
                cmd.replyTo()
                    .tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId())));
  }

  private Behavior<AgentCommand> onDeleteAgent(AgentCommand.DeleteAgent cmd) {
    try {
      boolean deleted = agentDao.delete(cmd.tenantId(), cmd.actorId());
      if (deleted) {
        cmd.replyTo().tell(AgentActorResponse.noContent());
      } else {
        cmd.replyTo().tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete actor", e);
    }
    return this;
  }

  private Behavior<AgentCommand> onMutateEndpoint(AgentCommand.MutateEndpoint cmd) {
    try {
      if (cmd.action() == AgentCommand.EndpointAction.ADD) {
        var req = new AddEndpointRequest(cmd.name(), cmd.endpoint().orElse(""));
        var validationError = AgentValidation.validateAddEndpoint(req);
        if (validationError.isPresent()) {
          cmd.replyTo().tell(AgentActorResponse.badRequest(validationError.orElseThrow()));
          return this;
        }
        boolean added =
            agentDao.addEndpoint(
                cmd.tenantId(), cmd.actorId(), cmd.name(), cmd.endpoint().orElse(""));
        if (added) {
          cmd.replyTo().tell(AgentActorResponse.noContent());
        } else {
          cmd.replyTo().tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId()));
        }
      } else {
        var req = new RemoveEndpointRequest(cmd.name());
        var validationError = AgentValidation.validateRemoveEndpoint(req);
        if (validationError.isPresent()) {
          cmd.replyTo().tell(AgentActorResponse.badRequest(validationError.orElseThrow()));
          return this;
        }
        boolean removed = agentDao.removeEndpoint(cmd.tenantId(), cmd.actorId(), cmd.name());
        if (removed) {
          cmd.replyTo().tell(AgentActorResponse.noContent());
        } else {
          cmd.replyTo().tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId()));
        }
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "mutate endpoint", e);
    }
    return this;
  }

  private Behavior<AgentCommand> onAccessCollection(AgentCommand.AccessCollection cmd) {
    try {
      if (cmd.action() == AgentCommand.CollectionAction.GET) {
        Optional<UUID> colIdOpt =
            cmd.target() == AgentCommand.CollectionTarget.INBOX
                ? agentDao.findInboxId(cmd.tenantId(), cmd.actorId())
                : agentDao.findOutboxId(cmd.tenantId(), cmd.actorId());
        String targetName =
            cmd.target() == AgentCommand.CollectionTarget.INBOX ? "Inbox" : "Outbox";
        resolveAndReturnCollection(
            cmd.tenantId(), cmd.actorId(), colIdOpt, targetName, cmd.replyTo());
      } else {
        if (agentDao.findById(cmd.tenantId(), cmd.actorId()).isEmpty()) {
          cmd.replyTo().tell(AgentActorResponse.notFound("Actor not found: " + cmd.actorId()));
        } else {
          cmd.replyTo().tell(AgentActorResponse.created());
        }
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "access collection", e);
    }
    return this;
  }

  private void resolveAndReturnCollection(
      UUID tenantId,
      UUID actorId,
      Optional<UUID> collectionIdOpt,
      String collectionName,
      ActorRef<AgentActorResponse> replyTo) {
    var actorOpt = agentDao.findById(tenantId, actorId);
    if (actorOpt.isEmpty()) {
      replyTo.tell(AgentActorResponse.notFound("Actor not found: " + actorId));
      return;
    }
    if (collectionIdOpt.isEmpty()) {
      replyTo.tell(
          AgentActorResponse.notFound(
              collectionName + " collection not found for actor: " + actorId));
      return;
    }
    var collectionId = collectionIdOpt.orElseThrow();
    var collectionOpt = collectionDao.findById(tenantId, collectionId);
    if (collectionOpt.isEmpty()) {
      replyTo.tell(
          AgentActorResponse.notFound(
              collectionName + " collection not found for actor: " + actorId));
      return;
    }
    var expandedItems = collectionDao.findExpandedItems(tenantId, collectionId);
    replyTo.tell(
        AgentActorResponse.collectionSuccess(
            toExpandedResponse(collectionOpt.orElseThrow(), expandedItems)));
  }

  private ActorResponse toActorResponse(String studioIdentifier, Agent agent) {
    var inboxUrl = "/api/studios/" + studioIdentifier + "/v1/actors/" + agent.id() + "/inbox";
    var outboxUrl = "/api/studios/" + studioIdentifier + "/v1/actors/" + agent.id() + "/outbox";
    return new ActorResponse(
        agent.id(),
        TYPE_ACTOR,
        agent.ownerId(),
        agent.preferredUsername(),
        agent.summary(),
        inboxUrl,
        outboxUrl,
        agent.endpoints(),
        agent.createdOn(),
        agent.updatedOn());
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
      ActorRef<AgentActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in AgentActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(AgentActorResponse.failure(reason));
  }
}
