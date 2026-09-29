package com.larpconnect.njall.api.admin.studios;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.DeletionFilter;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for studio administration operations. */
public sealed interface StudioAdminCommand {

  /**
   * Command instructing the actor to register a new studio with the given alias and optional name.
   */
  @Immutable
  record CreateStudio(String alias, Optional<String> name, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {

    public CreateStudio(String alias, ActorRef<StudioAdminResponse> replyTo) {
      this(alias, Optional.empty(), replyTo);
    }
  }

  /** Command instructing the actor to list registered studios. */
  @Immutable
  record ListStudios(DeletionFilter filter, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to find a studio by its unique UUID. */
  @Immutable
  record GetStudioById(UUID studioId, DeletionFilter filter, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to find a studio by its natural alias. */
  @Immutable
  record GetStudioByAlias(
      String alias, DeletionFilter filter, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}
}
