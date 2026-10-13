package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link AgentActor}. */
public sealed interface AgentCommand {

  enum EndpointAction {
    ADD,
    REMOVE
  }

  enum CollectionTarget {
    INBOX,
    OUTBOX
  }

  enum CollectionAction {
    GET,
    POST
  }

  @Immutable
  record CreateAgent(
      UUID tenantId,
      String studioIdentifier,
      CreateActorRequest request,
      ActorRef<AgentActorResponse> replyTo)
      implements AgentCommand {}

  @Immutable
  record GetAgent(
      UUID tenantId, String studioIdentifier, UUID actorId, ActorRef<AgentActorResponse> replyTo)
      implements AgentCommand {}

  @Immutable
  record PatchAgent(
      UUID tenantId,
      String studioIdentifier,
      UUID actorId,
      UpdateActorRequest request,
      Optional<String> updateMask,
      ActorRef<AgentActorResponse> replyTo)
      implements AgentCommand {}

  @Immutable
  record DeleteAgent(UUID tenantId, UUID actorId, ActorRef<AgentActorResponse> replyTo)
      implements AgentCommand {}

  @Immutable
  record MutateEndpoint(
      UUID tenantId,
      UUID actorId,
      EndpointAction action,
      String name,
      Optional<String> endpoint,
      ActorRef<AgentActorResponse> replyTo)
      implements AgentCommand {}

  @Immutable
  record AccessCollection(
      UUID tenantId,
      UUID actorId,
      CollectionTarget target,
      CollectionAction action,
      Optional<JsonNode> payload,
      ActorRef<AgentActorResponse> replyTo)
      implements AgentCommand {}
}
