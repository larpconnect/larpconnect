package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.AdminRoleDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Default implementation of {@link RoleAdminActorFactory} creating {@link RoleAdminActor}. */
final class DefaultRoleAdminActorFactory implements RoleAdminActorFactory {

  private final AdminRoleDAO roleDao;

  @Inject
  DefaultRoleAdminActorFactory(AdminRoleDAO roleDao) {
    this.roleDao = roleDao;
  }

  @Override
  public Behavior<RoleAdminCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private RoleAdminActor createActor(ActorContext<RoleAdminCommand> context) {
    return new RoleAdminActor(context, roleDao);
  }
}
