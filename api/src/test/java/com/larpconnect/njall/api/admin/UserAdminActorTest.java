package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.AdminRoleDAO;
import com.larpconnect.njall.data.dao.AdminUserDAO;
import com.larpconnect.njall.data.domain.AdminRole;
import com.larpconnect.njall.data.domain.AdminUser;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class UserAdminActorTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID roleId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<UserAdminCommand> createBehavior(AdminUserDAO userDao, AdminRoleDAO roleDao) {
    return Behaviors.setup(context -> new UserAdminActor(context, userDao, roleDao));
  }

  private AdminUser sampleUser(List<AdminRole> roles) {
    return AdminUser.of(userId, "admin_user", AdminUserStatus.ACTIVE, now, now, roles);
  }

  private AdminRole sampleRole() {
    return AdminRole.of(roleId, "security_admin");
  }

  @Test
  @DisplayName("onCreateUser creates user without initial roles")
  void onCreateUser_successWithoutRoles() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var user = sampleUser(List.of());

    when(userDao.findByUsername("admin_user")).thenReturn(Optional.empty());
    when(userDao.create("admin_user", AdminUserStatus.ACTIVE, List.of())).thenReturn(user);

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(
            "admin_user", AdminUserStatus.ACTIVE, null, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.UserSingle.class);
    assertThat(((UserAdminResponse.UserSingle) response).user().username()).isEqualTo("admin_user");
  }

  @Test
  @DisplayName("onCreateUser creates user with initial roles")
  void onCreateUser_successWithRoles() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    var userWithRole = sampleUser(List.of(role));

    when(userDao.findByUsername("admin_user")).thenReturn(Optional.empty());
    when(roleDao.findByRoleName("security_admin")).thenReturn(Optional.of(role));
    when(userDao.create("admin_user", AdminUserStatus.ACTIVE, List.of(roleId)))
        .thenReturn(userWithRole);

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(
            "admin_user", AdminUserStatus.ACTIVE, List.of("security_admin"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.UserSingle.class);
    assertThat(((UserAdminResponse.UserSingle) response).user().roles()).containsExactly(role);
  }

  @Test
  @DisplayName("onCreateUser rejects blank username")
  void onCreateUser_blankUsername_rejects() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.CreateUser("", AdminUserStatus.ACTIVE, null, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onCreateUser rejects duplicate username with conflict")
  void onCreateUser_duplicateUsername_conflict() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(sampleUser(List.of())));

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(
            "admin_user", AdminUserStatus.ACTIVE, null, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.Conflict.class);
  }

  @Test
  @DisplayName("onCreateUser rejects missing initial role")
  void onCreateUser_missingRole_rejects() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.empty());
    when(roleDao.findByRoleName("unknown_role")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(
            "admin_user", AdminUserStatus.ACTIVE, List.of("unknown_role"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onCreateUser replies with error on DAO failure")
  void onCreateUser_daoError() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername(anyString())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(
            "admin_user", AdminUserStatus.ACTIVE, null, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onListUsers returns all users")
  void onListUsers_success() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var user = sampleUser(List.of());
    when(userDao.list()).thenReturn(ImmutableList.of(user));

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.ListUsers(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.UserList.class);
    assertThat(((UserAdminResponse.UserList) response).users()).containsExactly(user);
  }

  @Test
  @DisplayName("onGetUserById returns user when found and notFound when missing")
  void onGetUserById_foundAndNotFound() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var user = sampleUser(List.of());
    when(userDao.findById(userId)).thenReturn(Optional.of(user));
    var missingId = UUID.randomUUID();
    when(userDao.findById(missingId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.GetUserById(userId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.UserSingle.class);

    testKit.run(new UserAdminCommand.GetUserById(missingId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetUserByUsername returns user when found and notFound when missing")
  void onGetUserByUsername_foundAndNotFound() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var user = sampleUser(List.of());
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(user));
    when(userDao.findByUsername("unknown")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.GetUserByUsername("admin_user", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.UserSingle.class);

    testKit.run(new UserAdminCommand.GetUserByUsername("unknown", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onAddRole assigns role or succeeds idempotently if already present")
  void onAddRole_successAndIdempotent() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    var userWithoutRole = sampleUser(List.of());
    var userWithRole = sampleUser(List.of(role));

    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(userWithoutRole));
    when(roleDao.findByRoleName("security_admin")).thenReturn(Optional.of(role));
    when(userDao.addRole(userId, roleId)).thenReturn(userWithRole);

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.AddRole("admin_user", null, "security_admin", inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.UserSingle.class);
    assertThat(((UserAdminResponse.UserSingle) response).user().roles()).containsExactly(role);

    // Idempotent test
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(userWithRole));
    testKit.run(new UserAdminCommand.AddRole("admin_user", null, "security_admin", inbox.getRef()));
    var response2 = inbox.receiveMessage();
    assertThat(response2).isInstanceOf(UserAdminResponse.UserSingle.class);
  }

  @Test
  @DisplayName("onAddRole returns notFound for missing user and badRequest for missing role")
  void onAddRole_missingUserAndMissingRole() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername("unknown")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.AddRole("unknown", null, "role", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.NotFound.class);

    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(sampleUser(List.of())));
    when(roleDao.findByRoleName("unknown_role")).thenReturn(Optional.empty());

    testKit.run(new UserAdminCommand.AddRole("admin_user", null, "unknown_role", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onRemoveRole removes role or succeeds idempotently if already absent")
  void onRemoveRole_successAndIdempotent() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    var userWithRole = sampleUser(List.of(role));
    var userWithoutRole = sampleUser(List.of());

    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(userWithRole));
    when(roleDao.findByRoleName("security_admin")).thenReturn(Optional.of(role));
    when(userDao.removeRole(userId, roleId)).thenReturn(userWithoutRole);

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.RemoveRole("admin_user", null, "security_admin", inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.UserSingle.class);
    assertThat(((UserAdminResponse.UserSingle) response).user().roles()).isEmpty();

    // Idempotent test
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(userWithoutRole));
    testKit.run(
        new UserAdminCommand.RemoveRole("admin_user", null, "security_admin", inbox.getRef()));
    var response2 = inbox.receiveMessage();
    assertThat(response2).isInstanceOf(UserAdminResponse.UserSingle.class);
  }

  @Test
  @DisplayName("onRemoveRole returns notFound for missing user and badRequest for missing role")
  void onRemoveRole_missingUserAndMissingRole() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername("unknown")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.RemoveRole("unknown", null, "role", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.NotFound.class);

    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(sampleUser(List.of())));
    when(roleDao.findByRoleName("unknown_role")).thenReturn(Optional.empty());

    testKit.run(
        new UserAdminCommand.RemoveRole("admin_user", null, "unknown_role", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("DefaultUserAdminActorFactory creates behavior successfully")
  void factory_createsBehavior() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var factory = new DefaultUserAdminActorFactory(userDao, roleDao);
    assertThat(factory.create()).isNotNull();
  }

  @Test
  @DisplayName("onCreateUser rejects null username and accepts explicit status")
  void onCreateUser_nullUsernameAndExplicitStatus() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(null, AdminUserStatus.ACTIVE, null, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);

    var disabledUser =
        AdminUser.of(userId, "disabled_user", AdminUserStatus.DISABLED, now, now, List.of());
    when(userDao.findByUsername("disabled_user")).thenReturn(Optional.empty());
    when(userDao.create("disabled_user", AdminUserStatus.DISABLED, List.of()))
        .thenReturn(disabledUser);

    testKit.run(
        new UserAdminCommand.CreateUser(
            "disabled_user", AdminUserStatus.DISABLED, null, inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.UserSingle.class);
    assertThat(((UserAdminResponse.UserSingle) response).user().status())
        .isEqualTo(AdminUserStatus.DISABLED);
  }

  @Test
  @DisplayName("onAddRole and onRemoveRole reject when both roleId and roleName are null")
  void onAddAndRemoveRole_nullRoleIdentifiers_rejects() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(sampleUser(List.of())));

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.AddRole("admin_user", null, null, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);

    testKit.run(new UserAdminCommand.RemoveRole("admin_user", null, null, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onAddRole and onRemoveRole handle missing role by UUID")
  void onAddAndRemoveRole_missingRoleByUuid() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var missingId = UUID.randomUUID();
    when(userDao.findByUsername("admin_user")).thenReturn(Optional.of(sampleUser(List.of())));
    when(roleDao.findById(missingId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(new UserAdminCommand.AddRole("admin_user", missingId, null, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);

    testKit.run(new UserAdminCommand.RemoveRole("admin_user", missingId, null, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(UserAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("handleError falls back to default reason when exception message is null")
  void onCreateUser_daoErrorWithNullMessage() {
    var userDao = mock(AdminUserDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    when(userDao.findByUsername(anyString())).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(userDao, roleDao));
    TestInbox<UserAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new UserAdminCommand.CreateUser(
            "admin_user", AdminUserStatus.ACTIVE, null, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(UserAdminResponse.Failure.class);
    assertThat(((UserAdminResponse.Failure) response).message())
        .contains("Error executing create user");
  }
}
