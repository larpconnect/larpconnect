package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.data.dao.AdminRoleDAO;
import com.larpconnect.njall.data.dao.AdminUserDAO;
import com.larpconnect.njall.data.domain.AdminRole;
import com.larpconnect.njall.data.domain.AdminUser;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user administration and role assignment operations. */
public final class UserAdminActor extends AbstractBehavior<UserAdminCommand> {

  private final Logger logger = LoggerFactory.getLogger(UserAdminActor.class);
  private final AdminUserDAO userDao;
  private final AdminRoleDAO roleDao;

  public UserAdminActor(
      ActorContext<UserAdminCommand> context, AdminUserDAO userDao, AdminRoleDAO roleDao) {
    super(context);
    this.userDao = userDao;
    this.roleDao = roleDao;
  }

  @Override
  public Receive<UserAdminCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(UserAdminCommand.CreateUser.class, this::onCreateUser)
        .onMessage(UserAdminCommand.ListUsers.class, this::onListUsers)
        .onMessage(UserAdminCommand.GetUserById.class, this::onGetUserById)
        .onMessage(UserAdminCommand.GetUserByUsername.class, this::onGetUserByUsername)
        .onMessage(UserAdminCommand.AddRole.class, this::onAddRole)
        .onMessage(UserAdminCommand.RemoveRole.class, this::onRemoveRole)
        .build();
  }

  private Behavior<UserAdminCommand> onCreateUser(UserAdminCommand.CreateUser cmd) {
    try {
      if (cmd.username() == null || cmd.username().isBlank()) {
        cmd.replyTo().tell(UserAdminResponse.badRequest("Username cannot be blank"));
        return this;
      }
      var existing = userDao.findByUsername(cmd.username());
      if (existing.isPresent()) {
        cmd.replyTo()
            .tell(UserAdminResponse.conflict("Username already exists: " + cmd.username()));
        return this;
      }
      var resolvedRoles = new ArrayList<AdminRole>();
      if (cmd.initialRoles() != null) {
        for (var roleStr : cmd.initialRoles()) {
          var maybeRole = resolveRoleByIdentifier(roleStr);
          if (maybeRole.isEmpty()) {
            cmd.replyTo().tell(UserAdminResponse.badRequest("Role not found: " + roleStr));
            return this;
          }
          resolvedRoles.add(maybeRole.get());
        }
      }
      var roleIds = resolvedRoles.stream().map(AdminRole::id).toList();
      var status = cmd.status() != null ? cmd.status() : AdminUserStatus.ACTIVE;
      var user = userDao.create(cmd.username(), status, roleIds);
      cmd.replyTo().tell(UserAdminResponse.single(user));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create user", e);
    }
    return this;
  }

  private Behavior<UserAdminCommand> onListUsers(UserAdminCommand.ListUsers cmd) {
    try {
      var users = userDao.list();
      cmd.replyTo().tell(UserAdminResponse.list(users));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list users", e);
    }
    return this;
  }

  private Behavior<UserAdminCommand> onGetUserById(UserAdminCommand.GetUserById cmd) {
    try {
      var user = userDao.findById(cmd.userId());
      if (user.isPresent()) {
        cmd.replyTo().tell(UserAdminResponse.single(user.get()));
      } else {
        cmd.replyTo().tell(UserAdminResponse.notFound("User not found: " + cmd.userId()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get user by id", e);
    }
    return this;
  }

  private Behavior<UserAdminCommand> onGetUserByUsername(UserAdminCommand.GetUserByUsername cmd) {
    try {
      var user = userDao.findByUsername(cmd.username());
      if (user.isPresent()) {
        cmd.replyTo().tell(UserAdminResponse.single(user.get()));
      } else {
        cmd.replyTo().tell(UserAdminResponse.notFound("User not found: " + cmd.username()));
      }
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get user by username", e);
    }
    return this;
  }

  private Behavior<UserAdminCommand> onAddRole(UserAdminCommand.AddRole cmd) {
    try {
      var maybeUser = resolveUser(cmd.userIdentifier());
      if (maybeUser.isEmpty()) {
        cmd.replyTo().tell(UserAdminResponse.notFound("User not found: " + cmd.userIdentifier()));
        return this;
      }
      var maybeRole = resolveRole(cmd.roleId(), cmd.roleName());
      if (maybeRole.isEmpty()) {
        var missing = cmd.roleId() != null ? cmd.roleId().toString() : cmd.roleName();
        cmd.replyTo().tell(UserAdminResponse.badRequest("Role not found: " + missing));
        return this;
      }
      var user = maybeUser.get();
      var role = maybeRole.get();
      boolean alreadyHas = user.roles().stream().anyMatch(r -> r.id().equals(role.id()));
      if (alreadyHas) {
        cmd.replyTo().tell(UserAdminResponse.single(user));
        return this;
      }
      var updated = userDao.addRole(user.id(), role.id());
      cmd.replyTo().tell(UserAdminResponse.single(updated));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "add role", e);
    }
    return this;
  }

  private Behavior<UserAdminCommand> onRemoveRole(UserAdminCommand.RemoveRole cmd) {
    try {
      var maybeUser = resolveUser(cmd.userIdentifier());
      if (maybeUser.isEmpty()) {
        cmd.replyTo().tell(UserAdminResponse.notFound("User not found: " + cmd.userIdentifier()));
        return this;
      }
      var maybeRole = resolveRole(cmd.roleId(), cmd.roleName());
      if (maybeRole.isEmpty()) {
        var missing = cmd.roleId() != null ? cmd.roleId().toString() : cmd.roleName();
        cmd.replyTo().tell(UserAdminResponse.badRequest("Role not found: " + missing));
        return this;
      }
      var user = maybeUser.get();
      var role = maybeRole.get();
      boolean has = user.roles().stream().anyMatch(r -> r.id().equals(role.id()));
      if (!has) {
        cmd.replyTo().tell(UserAdminResponse.single(user));
        return this;
      }
      var updated = userDao.removeRole(user.id(), role.id());
      cmd.replyTo().tell(UserAdminResponse.single(updated));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "remove role", e);
    }
    return this;
  }

  private Optional<AdminUser> resolveUser(String identifier) {
    var maybeUuid = AdminValidation.tryParseUuid(identifier);
    return maybeUuid.map(userDao::findById).orElseGet(() -> userDao.findByUsername(identifier));
  }

  private Optional<AdminRole> resolveRole(@Nullable UUID roleId, @Nullable String roleName) {
    if (roleId != null) {
      return roleDao.findById(roleId);
    }
    if (roleName != null) {
      return roleDao.findByRoleName(roleName);
    }
    return Optional.empty();
  }

  private Optional<AdminRole> resolveRoleByIdentifier(String identifier) {
    var maybeUuid = AdminValidation.tryParseUuid(identifier);
    return maybeUuid.map(roleDao::findById).orElseGet(() -> roleDao.findByRoleName(identifier));
  }

  private void handleError(ActorRef<UserAdminResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in UserAdminActor", operation, error);
    var message = error.getMessage();
    var reason = message != null && !message.isBlank() ? message : "Error executing " + operation;
    replyTo.tell(UserAdminResponse.failure(reason));
  }
}
