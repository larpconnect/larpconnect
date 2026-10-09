package com.larpconnect.njall.api.admin.studioroles;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.admin.StudioRoleDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/**
 * Default implementation of {@link StudioRoleAdminActorFactory} creating {@link
 * StudioRoleAdminActor}.
 */
final class DefaultStudioRoleAdminActorFactory implements StudioRoleAdminActorFactory {

  private final StudioRoleDAO roleDao;

  @Inject
  DefaultStudioRoleAdminActorFactory(StudioRoleDAO roleDao) {
    this.roleDao = roleDao;
  }

  @Override
  public Behavior<StudioRoleAdminCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private StudioRoleAdminActor createActor(ActorContext<StudioRoleAdminCommand> context) {
    return new StudioRoleAdminActor(context, roleDao);
  }
}
