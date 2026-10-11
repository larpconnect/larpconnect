package com.larpconnect.njall.api.studios.individuals;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link IndividualActor}. */
public sealed interface IndividualCommand {

  @Immutable
  record CreateIndividual(
      UUID tenantId, CreateIndividualRequest request, ActorRef<IndividualActorResponse> replyTo)
      implements IndividualCommand {}

  @Immutable
  record GetIndividual(UUID tenantId, UUID individualId, ActorRef<IndividualActorResponse> replyTo)
      implements IndividualCommand {}

  @Immutable
  record PatchIndividual(
      UUID tenantId,
      UUID individualId,
      UpdateIndividualRequest request,
      Optional<String> updateMask,
      ActorRef<IndividualActorResponse> replyTo)
      implements IndividualCommand {}

  @Immutable
  record DeleteIndividual(
      UUID tenantId, UUID individualId, ActorRef<IndividualActorResponse> replyTo)
      implements IndividualCommand {}
}
