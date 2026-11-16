package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Pekko Typed command protocol for inbox activity processing. */
public sealed interface InboxCommand
    permits InboxCommand.DeliverActivity, InboxCommand.DeliverBatch, InboxActor.CompleteDelivery {

  /** Command to process and queue a single FeatherPub activity. */
  @Immutable
  record DeliverActivity(
      UUID tenantId,
      UUID studioId,
      String studioIdParam,
      String actorIdentifier,
      JsonNode activity,
      Optional<String> traceparent,
      ActorRef<InboxActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements InboxCommand {

    public DeliverActivity {
      activity = activity.deepCopy();
    }

    @Override
    public JsonNode activity() {
      return activity.deepCopy();
    }
  }

  /** Command to process and queue an AIP-233 batch of FeatherPub activities. */
  @Immutable
  record DeliverBatch(
      UUID tenantId,
      UUID studioId,
      String studioIdParam,
      String actorIdentifier,
      BatchInboxActivitiesRequest request,
      Optional<String> traceparent,
      ActorRef<InboxActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements InboxCommand {}
}
