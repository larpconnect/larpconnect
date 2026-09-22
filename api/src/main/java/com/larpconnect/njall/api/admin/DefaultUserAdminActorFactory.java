package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.AdminRoleDAO;
import com.larpconnect.njall.data.dao.AdminUserDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Default implementation of {@link UserAdminActorFactory} creating {@link UserAdminActor}. */
final class DefaultUserAdminActorFactory implements UserAdminActorFactory {

  private final AdminUserDAO userDao;
  private final AdminRoleDAO roleDao;

  @Inject
  DefaultUserAdminActorFactory(AdminUserDAO userDao, AdminRoleDAO roleDao) {
    this.userDao = userDao;
    this.roleDao = roleDao;
  }

  @Override
  public Behavior<UserAdminCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private UserAdminActor createActor(ActorContext<UserAdminCommand> context) {
    return new UserAdminActor(context, userDao, roleDao);
  }
}
