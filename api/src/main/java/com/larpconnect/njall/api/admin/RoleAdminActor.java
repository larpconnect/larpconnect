package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.data.dao.AdminRoleDAO;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing administrative role management operations. */
public final class RoleAdminActor extends AbstractBehavior<RoleAdminCommand> {

  private final Logger logger = LoggerFactory.getLogger(RoleAdminActor.class);
  private final AdminRoleDAO roleDao;

  public RoleAdminActor(ActorContext<RoleAdminCommand> context, AdminRoleDAO roleDao) {
    super(context);
    this.roleDao = roleDao;
  }

  @Override
  public Receive<RoleAdminCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(RoleAdminCommand.CreateRole.class, this::onCreateRole)
        .onMessage(RoleAdminCommand.ListRoles.class, this::onListRoles)
        .onMessage(RoleAdminCommand.GetRoleById.class, this::onGetRoleById)
        .onMessage(RoleAdminCommand.GetRoleByName.class, this::onGetRoleByName)
        .build();
  }

  private Behavior<RoleAdminCommand> onCreateRole(RoleAdminCommand.CreateRole cmd) {
    try {
      if (!AdminValidation.isValidIdentifier(cmd.roleName())) {
        cmd.replyTo().tell(RoleAdminResponse.badRequest("Invalid role name: " + cmd.roleName()));
        return this;
      }
      var existing = roleDao.findByRoleName(cmd.roleName());
      if (existing.isPresent()) {
        cmd.replyTo().tell(RoleAdminResponse.conflict("Role already exists: " + cmd.roleName()));
        return this;
      }
      var role = roleDao.create(cmd.roleName());
      cmd.replyTo().tell(RoleAdminResponse.single(role));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create role", e);
    }
    return this;
  }

  private Behavior<RoleAdminCommand> onListRoles(RoleAdminCommand.ListRoles cmd) {
    try {
      var roles = roleDao.list();
      cmd.replyTo().tell(RoleAdminResponse.list(roles));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list roles", e);
    }
    return this;
  }

  private Behavior<RoleAdminCommand> onGetRoleById(RoleAdminCommand.GetRoleById cmd) {
    try {
      var role = roleDao.findById(cmd.roleId());
      if (role.isPresent()) {
        cmd.replyTo().tell(RoleAdminResponse.single(role.get()));
      } else {
        cmd.replyTo().tell(RoleAdminResponse.notFound("Role not found: " + cmd.roleId()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get role by id", e);
    }
    return this;
  }

  private Behavior<RoleAdminCommand> onGetRoleByName(RoleAdminCommand.GetRoleByName cmd) {
    try {
      var role = roleDao.findByRoleName(cmd.roleName());
      if (role.isPresent()) {
        cmd.replyTo().tell(RoleAdminResponse.single(role.get()));
      } else {
        cmd.replyTo().tell(RoleAdminResponse.notFound("Role not found: " + cmd.roleName()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get role by name", e);
    }
    return this;
  }

  private void handleError(ActorRef<RoleAdminResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in RoleAdminActor", operation, error);
    var message = error.getMessage();
    var reason = message != null && !message.isBlank() ? message : "Error executing " + operation;
    replyTo.tell(RoleAdminResponse.failure(reason));
  }
}
