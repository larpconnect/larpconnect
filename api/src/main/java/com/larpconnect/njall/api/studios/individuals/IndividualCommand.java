package com.larpconnect.njall.api.studios.individuals;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link IndividualActor}. */
public sealed interface IndividualCommand {

  @Immutable
  record CreateIndividual(
      UUID tenantId,
      String studioIdentifier,
      CreateIndividualRequest request,
      ActorRef<IndividualActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements IndividualCommand {
    public CreateIndividual(
        UUID tenantId,
        String studioIdentifier,
        CreateIndividualRequest request,
        ActorRef<IndividualActorResponse> replyTo) {
      this(tenantId, studioIdentifier, request, replyTo, Optional.empty());
    }
  }

  @Immutable
  record GetIndividual(
      UUID tenantId,
      String studioIdentifier,
      UUID individualId,
      ActorRef<IndividualActorResponse> replyTo)
      implements IndividualCommand {}

  @Immutable
  record PatchIndividual(
      UUID tenantId,
      String studioIdentifier,
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
