package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.Behavior;

/** Factory interface for creating {@link StudioRoleAdminActor} behavior instances. */
public interface StudioRoleAdminActorFactory {

  /**
   * Creates an initial behavior for the default studio role admin actor.
   *
   * @return A behavior handling {@link StudioRoleAdminCommand} messages.
   */
  Behavior<StudioRoleAdminCommand> create();
}
