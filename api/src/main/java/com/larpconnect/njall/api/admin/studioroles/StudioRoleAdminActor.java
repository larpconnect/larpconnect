package com.larpconnect.njall.api.admin.studioroles;

import com.google.common.base.Strings;
import com.larpconnect.njall.data.dao.DefaultStudioRoleDAO;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing default studio role management operations. */
public final class StudioRoleAdminActor extends AbstractBehavior<StudioRoleAdminCommand> {

  private final Logger logger = LoggerFactory.getLogger(StudioRoleAdminActor.class);
  private final DefaultStudioRoleDAO roleDao;

  StudioRoleAdminActor(ActorContext<StudioRoleAdminCommand> context, DefaultStudioRoleDAO roleDao) {
    super(context);
    this.roleDao = roleDao;
  }

  @Override
  public Receive<StudioRoleAdminCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(StudioRoleAdminCommand.CreateRole.class, this::onCreateRole)
        .onMessage(StudioRoleAdminCommand.ListRoles.class, this::onListRoles)
        .onMessage(StudioRoleAdminCommand.GetRoleById.class, this::onGetRoleById)
        .onMessage(StudioRoleAdminCommand.UpdateRole.class, this::onUpdateRole)
        .build();
  }

  private Behavior<StudioRoleAdminCommand> onCreateRole(StudioRoleAdminCommand.CreateRole cmd) {
    try {
      processCreateRole(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create default studio role", e);
    }
    return this;
  }

  private void processCreateRole(StudioRoleAdminCommand.CreateRole cmd) {
    if (cmd.name().isBlank()) {
      replyBadRequest(cmd.replyTo(), "Role name cannot be blank");
      return;
    }
    if (hasExistingRoleName(cmd.name())) {
      replyConflict(cmd.replyTo(), "Default studio role already exists: " + cmd.name());
      return;
    }
    applyRoleCreate(cmd.name(), cmd.replyTo());
  }

  private boolean hasExistingRoleName(String name) {
    return roleDao.findByName(name).isPresent();
  }

  private void applyRoleCreate(String name, ActorRef<StudioRoleAdminResponse> replyTo) {
    var role = roleDao.create(name);
    replyTo.tell(StudioRoleAdminResponse.single(role));
  }

  private Behavior<StudioRoleAdminCommand> onListRoles(StudioRoleAdminCommand.ListRoles cmd) {
    try {
      executeListRoles(cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list default studio roles", e);
    }
    return this;
  }

  private void executeListRoles(ActorRef<StudioRoleAdminResponse> replyTo) {
    var roles = roleDao.list();
    replyTo.tell(StudioRoleAdminResponse.list(roles));
  }

  private Behavior<StudioRoleAdminCommand> onGetRoleById(StudioRoleAdminCommand.GetRoleById cmd) {
    try {
      executeGetRoleById(cmd.roleId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get default studio role by id", e);
    }
    return this;
  }

  private void executeGetRoleById(UUID roleId, ActorRef<StudioRoleAdminResponse> replyTo) {
    roleDao
        .findById(roleId)
        .ifPresentOrElse(
            role -> replyTo.tell(StudioRoleAdminResponse.single(role)),
            () ->
                replyTo.tell(
                    StudioRoleAdminResponse.notFound("Default studio role not found: " + roleId)));
  }

  private Behavior<StudioRoleAdminCommand> onUpdateRole(StudioRoleAdminCommand.UpdateRole cmd) {
    try {
      processUpdateRole(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "update default studio role", e);
    }
    return this;
  }

  private void processUpdateRole(StudioRoleAdminCommand.UpdateRole cmd) {
    if (!shouldUpdateName(cmd.updateMask())) {
      handleUnchangedRole(cmd.roleId(), cmd.replyTo());
      return;
    }
    if (cmd.name().isBlank()) {
      replyBadRequest(cmd.replyTo(), "Role name cannot be blank");
      return;
    }
    if (hasNameConflict(cmd.roleId(), cmd.name())) {
      replyConflict(cmd.replyTo(), "Default studio role already exists: " + cmd.name());
      return;
    }
    applyRoleUpdate(cmd.roleId(), cmd.name(), cmd.replyTo());
  }

  private boolean shouldUpdateName(Optional<String> updateMask) {
    return updateMask.map(mask -> mask.contains("name")).orElse(true);
  }

  private void handleUnchangedRole(UUID roleId, ActorRef<StudioRoleAdminResponse> replyTo) {
    roleDao
        .findById(roleId)
        .ifPresentOrElse(
            existing -> replyTo.tell(StudioRoleAdminResponse.single(existing)),
            () ->
                replyTo.tell(
                    StudioRoleAdminResponse.notFound("Default studio role not found: " + roleId)));
  }

  private boolean hasNameConflict(UUID roleId, String name) {
    return roleDao.findByName(name).map(r -> !r.id().equals(roleId)).orElse(false);
  }

  private void applyRoleUpdate(
      UUID roleId, String name, ActorRef<StudioRoleAdminResponse> replyTo) {
    roleDao
        .update(roleId, name)
        .ifPresentOrElse(
            updated -> replyTo.tell(StudioRoleAdminResponse.single(updated)),
            () ->
                replyTo.tell(
                    StudioRoleAdminResponse.notFound("Default studio role not found: " + roleId)));
  }

  private void replyBadRequest(ActorRef<StudioRoleAdminResponse> replyTo, String message) {
    replyTo.tell(StudioRoleAdminResponse.badRequest(message));
  }

  private void replyConflict(ActorRef<StudioRoleAdminResponse> replyTo, String message) {
    replyTo.tell(StudioRoleAdminResponse.conflict(message));
  }

  private void handleError(
      ActorRef<StudioRoleAdminResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in StudioRoleAdminActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(StudioRoleAdminResponse.failure(reason));
  }
}
