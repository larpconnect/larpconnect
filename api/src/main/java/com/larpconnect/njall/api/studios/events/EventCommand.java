package com.larpconnect.njall.api.studios.events;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link EventActor}. */
public sealed interface EventCommand {

  @Immutable
  record CreateEvent(
      UUID tenantId,
      String studioIdentifier,
      CreateEventRequest request,
      ActorRef<EventActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements EventCommand {
    public CreateEvent(
        UUID tenantId,
        String studioIdentifier,
        CreateEventRequest request,
        ActorRef<EventActorResponse> replyTo) {
      this(tenantId, studioIdentifier, request, replyTo, Optional.empty());
    }
  }

  @Immutable
  record GetEvent(
      UUID tenantId, String studioIdentifier, UUID eventId, ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}

  @Immutable
  record ListEvents(UUID tenantId, String studioIdentifier, ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}

  @Immutable
  record PatchEvent(
      UUID tenantId,
      String studioIdentifier,
      UUID eventId,
      UpdateEventRequest request,
      Optional<String> updateMask,
      ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}

  @Immutable
  record DeleteEvent(UUID tenantId, UUID eventId, ActorRef<EventActorResponse> replyTo)
      implements EventCommand {}
}
