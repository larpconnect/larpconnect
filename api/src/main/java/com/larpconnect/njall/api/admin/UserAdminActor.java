package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.dao.AdminRoleDAO;
import com.larpconnect.njall.data.dao.AdminUserDAO;
import com.larpconnect.njall.data.domain.AdminRole;
import com.larpconnect.njall.data.domain.AdminUser;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
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
      var validationError = validateCreateUserCommand(cmd);
      if (validationError.isPresent()) {
        cmd.replyTo().tell(validationError.get());
        return this;
      }
      return switch (resolveRoleIds(cmd.initialRoles())) {
        case RoleResolution.MissingRole missing -> {
          cmd.replyTo()
              .tell(UserAdminResponse.badRequest("Role not found: " + missing.roleIdentifier()));
          yield this;
        }
        case RoleResolution.Success success -> {
          var status =
              cmd.status() == AdminUserStatus.UNKNOWN ? AdminUserStatus.ACTIVE : cmd.status();
          var user = userDao.create(cmd.username(), status, success.roleIds());
          cmd.replyTo().tell(UserAdminResponse.single(user));
          yield this;
        }
      };
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
        var missing = cmd.roleId().map(UUID::toString).or(cmd::roleName).orElse("unspecified");
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
        var missing = cmd.roleId().map(UUID::toString).or(cmd::roleName).orElse("unspecified");
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

  private Optional<AdminRole> resolveRole(Optional<UUID> roleId, Optional<String> roleName) {
    if (roleId.isPresent()) {
      return roleDao.findById(roleId.get());
    }
    if (roleName.isPresent()) {
      return roleDao.findByRoleName(roleName.get());
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

  private Optional<UserAdminResponse> validateCreateUserCommand(UserAdminCommand.CreateUser cmd) {
    if (cmd.username().isBlank()) {
      return Optional.of(UserAdminResponse.badRequest("Username cannot be blank"));
    }
    if (userDao.findByUsername(cmd.username()).isPresent()) {
      return Optional.of(UserAdminResponse.conflict("Username already exists: " + cmd.username()));
    }
    return Optional.empty();
  }

  private RoleResolution resolveRoleIds(ImmutableList<String> initialRoles) {
    if (initialRoles.isEmpty()) {
      return new RoleResolution.Success(ImmutableList.of());
    }
    var roleIds = ImmutableList.<UUID>builder();
    for (var roleStr : initialRoles) {
      var maybeRole = resolveRoleByIdentifier(roleStr);
      if (maybeRole.isEmpty()) {
        return new RoleResolution.MissingRole(roleStr);
      }
      roleIds.add(maybeRole.get().id());
    }
    return new RoleResolution.Success(roleIds.build());
  }

  private sealed interface RoleResolution {
    @Immutable
    record Success(ImmutableList<UUID> roleIds) implements RoleResolution {}

    @Immutable
    record MissingRole(String roleIdentifier) implements RoleResolution {}
  }
}
