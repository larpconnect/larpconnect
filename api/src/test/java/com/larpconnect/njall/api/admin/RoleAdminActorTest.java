package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.AdminRoleDAO;
import com.larpconnect.njall.data.domain.AdminRole;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class RoleAdminActorTest {

  private final UUID roleId = UUID.randomUUID();

  private Behavior<RoleAdminCommand> createBehavior(AdminRoleDAO roleDao) {
    return Behaviors.setup(context -> new RoleAdminActor(context, roleDao));
  }

  private AdminRole sampleRole() {
    return AdminRole.of(roleId, "security_admin");
  }

  @Test
  @DisplayName("onCreateRole creates role when roleName is valid and unique")
  void onCreateRole_success() {
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    when(roleDao.findByRoleName("security_admin")).thenReturn(Optional.empty());
    when(roleDao.create("security_admin")).thenReturn(role);

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.CreateRole("security_admin", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.RoleSingle.class);
    assertThat(((RoleAdminResponse.RoleSingle) response).role().roleName())
        .isEqualTo("security_admin");
  }

  @Test
  @DisplayName("onCreateRole rejects invalid roleName format")
  void onCreateRole_invalidRoleName_rejects() {
    var roleDao = mock(AdminRoleDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.CreateRole("Role-Invalid", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onCreateRole rejects duplicate roleName with conflict")
  void onCreateRole_duplicateRole_conflict() {
    var roleDao = mock(AdminRoleDAO.class);
    when(roleDao.findByRoleName("security_admin")).thenReturn(Optional.of(sampleRole()));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.CreateRole("security_admin", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.Conflict.class);
  }

  @Test
  @DisplayName("onCreateRole replies with error when DAO throws exception")
  void onCreateRole_daoError() {
    var roleDao = mock(AdminRoleDAO.class);
    when(roleDao.findByRoleName(anyString())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.CreateRole("security_admin", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onListRoles returns all roles")
  void onListRoles_success() {
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    when(roleDao.list()).thenReturn(ImmutableList.of(role));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.ListRoles(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.RoleList.class);
    assertThat(((RoleAdminResponse.RoleList) response).roles()).containsExactly(role);
  }

  @Test
  @DisplayName("onListRoles replies with error on DAO failure")
  void onListRoles_daoError() {
    var roleDao = mock(AdminRoleDAO.class);
    when(roleDao.list()).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.ListRoles(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onGetRoleById returns single when found and notFound when missing")
  void onGetRoleById_foundAndNotFound() {
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    when(roleDao.findById(roleId)).thenReturn(Optional.of(role));
    var missingId = UUID.randomUUID();
    when(roleDao.findById(missingId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.GetRoleById(roleId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(RoleAdminResponse.RoleSingle.class);

    testKit.run(new RoleAdminCommand.GetRoleById(missingId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(RoleAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetRoleById replies with error on DAO failure")
  void onGetRoleById_daoError() {
    var roleDao = mock(AdminRoleDAO.class);
    when(roleDao.findById(any())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.GetRoleById(roleId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(RoleAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onGetRoleByName returns single when found and notFound when missing")
  void onGetRoleByName_foundAndNotFound() {
    var roleDao = mock(AdminRoleDAO.class);
    var role = sampleRole();
    when(roleDao.findByRoleName("security_admin")).thenReturn(Optional.of(role));
    when(roleDao.findByRoleName("unknown")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.GetRoleByName("security_admin", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(RoleAdminResponse.RoleSingle.class);

    testKit.run(new RoleAdminCommand.GetRoleByName("unknown", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(RoleAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetRoleByName replies with error on DAO failure")
  void onGetRoleByName_daoError() {
    var roleDao = mock(AdminRoleDAO.class);
    when(roleDao.findByRoleName(anyString())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.GetRoleByName("security_admin", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(RoleAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("DefaultRoleAdminActorFactory creates behavior successfully")
  void factory_createsBehavior() {
    var roleDao = mock(AdminRoleDAO.class);
    var factory = new DefaultRoleAdminActorFactory(roleDao);
    assertThat(factory.create()).isNotNull();
  }

  @Test
  @DisplayName("handleError falls back to default reason when exception message is null")
  void onCreateRole_daoErrorWithNullMessage() {
    var roleDao = mock(AdminRoleDAO.class);
    when(roleDao.findByRoleName(anyString())).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<RoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new RoleAdminCommand.CreateRole("security_admin", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(RoleAdminResponse.Failure.class);
    assertThat(((RoleAdminResponse.Failure) response).message())
        .contains("Error executing create role");
  }
}
