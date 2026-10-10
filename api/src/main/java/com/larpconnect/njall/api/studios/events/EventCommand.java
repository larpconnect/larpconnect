package com.larpconnect.njall.api.studios.events;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link EventActor}. */
public sealed interface EventCommand {

  @Immutable
  record CreateEvent(
      UUID tenantId, CreateEventRequest request, ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}

  @Immutable
  record GetEvent(UUID tenantId, UUID eventId, ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}

  @Immutable
  record ListEvents(UUID tenantId, ActorRef<EventActorResponse> replyTo) implements EventCommand {}

  @Immutable
  record PatchEvent(
      UUID tenantId,
      UUID eventId,
      UpdateEventRequest request,
      Optional<String> updateMask,
      ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}

  @Immutable
  record DeleteEvent(UUID tenantId, UUID eventId, ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}
}
