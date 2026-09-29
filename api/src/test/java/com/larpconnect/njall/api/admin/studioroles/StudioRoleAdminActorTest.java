package com.larpconnect.njall.api.admin.studioroles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.DefaultStudioRoleDAO;
import com.larpconnect.njall.data.domain.DefaultStudioRole;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StudioRoleAdminActorTest {

  private final UUID roleId = UUID.randomUUID();

  private Behavior<StudioRoleAdminCommand> createBehavior(DefaultStudioRoleDAO roleDao) {
    return Behaviors.setup(context -> new StudioRoleAdminActor(context, roleDao));
  }

  private DefaultStudioRole sampleRole() {
    return new DefaultStudioRole(roleId, "ORGANIZER");
  }

  @Test
  @DisplayName("onCreateRole creates role when name is valid and unique")
  void onCreateRole_success() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var role = sampleRole();
    when(roleDao.findByName("ORGANIZER")).thenReturn(Optional.empty());
    when(roleDao.create("ORGANIZER")).thenReturn(role);

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.CreateRole("ORGANIZER", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.RoleSingle.class);
    assertThat(((StudioRoleAdminResponse.RoleSingle) response).role().name())
        .isEqualTo("ORGANIZER");
  }

  @Test
  @DisplayName("onCreateRole rejects blank name")
  void onCreateRole_blankName_rejects() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.CreateRole("   ", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onCreateRole returns conflict when name already exists")
  void onCreateRole_duplicate_conflict() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findByName("ORGANIZER")).thenReturn(Optional.of(sampleRole()));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.CreateRole("ORGANIZER", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.Conflict.class);
  }

  @Test
  @DisplayName("onListRoles returns list of roles")
  void onListRoles_success() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var role = sampleRole();
    when(roleDao.list()).thenReturn(ImmutableList.of(role));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.ListRoles(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.RoleList.class);
    assertThat(((StudioRoleAdminResponse.RoleList) response).roles()).hasSize(1);
  }

  @Test
  @DisplayName("onGetRoleById returns role when present")
  void onGetRoleById_found() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var role = sampleRole();
    when(roleDao.findById(roleId)).thenReturn(Optional.of(role));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.GetRoleById(roleId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.RoleSingle.class);
  }

  @Test
  @DisplayName("onGetRoleById returns notFound when missing")
  void onGetRoleById_notFound() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findById(roleId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.GetRoleById(roleId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onUpdateRole updates role name successfully conforming to AIP-134")
  void onUpdateRole_success() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var updatedRole = new DefaultStudioRole(roleId, "LEAD_ORGANIZER");
    when(roleDao.findByName("LEAD_ORGANIZER")).thenReturn(Optional.empty());
    when(roleDao.update(roleId, "LEAD_ORGANIZER")).thenReturn(Optional.of(updatedRole));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(
            roleId, "LEAD_ORGANIZER", Optional.of("name"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.RoleSingle.class);
    assertThat(((StudioRoleAdminResponse.RoleSingle) response).role().name())
        .isEqualTo("LEAD_ORGANIZER");
  }

  @Test
  @DisplayName("onUpdateRole ignores name when mask does not include name")
  void onUpdateRole_maskWithoutName_returnsExisting() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var existingRole = sampleRole();
    when(roleDao.findById(roleId)).thenReturn(Optional.of(existingRole));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(
            roleId, "NEW_NAME", Optional.of("other_field"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.RoleSingle.class);
    assertThat(((StudioRoleAdminResponse.RoleSingle) response).role().name())
        .isEqualTo("ORGANIZER");
  }

  @Test
  @DisplayName("onUpdateRole rejects blank name")
  void onUpdateRole_blankName_rejects() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(roleId, "   ", Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onUpdateRole returns conflict when name belongs to another role")
  void onUpdateRole_conflict() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var anotherRole = new DefaultStudioRole(UUID.randomUUID(), "TAKEN");
    when(roleDao.findByName("TAKEN")).thenReturn(Optional.of(anotherRole));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(roleId, "TAKEN", Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.Conflict.class);
  }

  @Test
  @DisplayName("onUpdateRole returns notFound when role does not exist")
  void onUpdateRole_notFound() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findByName("NEW_NAME")).thenReturn(Optional.empty());
    when(roleDao.update(roleId, "NEW_NAME")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(
            roleId, "NEW_NAME", Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onUpdateRole allows keeping the same name for the same role")
  void onUpdateRole_sameNameSameRole_success() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var existingRole = sampleRole();
    when(roleDao.findByName("ORGANIZER")).thenReturn(Optional.of(existingRole));
    when(roleDao.update(roleId, "ORGANIZER")).thenReturn(Optional.of(existingRole));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(
            roleId, "ORGANIZER", Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.RoleSingle.class);
  }

  @Test
  @DisplayName("onUpdateRole returns notFound when mask does not include name and role missing")
  void onUpdateRole_maskWithoutName_notFound() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findById(roleId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(
            roleId, "ANY", Optional.of("other_field"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onCreateRole handles DAO exception with message")
  void onCreateRole_daoError() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findByName("ORGANIZER")).thenThrow(new RuntimeException("DB error"));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.CreateRole("ORGANIZER", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.Failure.class);
    assertThat(((StudioRoleAdminResponse.Failure) response).message()).isEqualTo("DB error");
  }

  @Test
  @DisplayName("onListRoles handles DAO exception with null message")
  void onListRoles_daoErrorNullMessage() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.list()).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.ListRoles(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.Failure.class);
    assertThat(((StudioRoleAdminResponse.Failure) response).message())
        .contains("Error executing list default studio roles");
  }

  @Test
  @DisplayName("onGetRoleById handles DAO exception with empty message")
  void onGetRoleById_daoErrorEmptyMessage() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findById(roleId)).thenThrow(new RuntimeException(""));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioRoleAdminCommand.GetRoleById(roleId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.Failure.class);
    assertThat(((StudioRoleAdminResponse.Failure) response).message())
        .contains("Error executing get default studio role by id");
  }

  @Test
  @DisplayName("onUpdateRole handles DAO exception")
  void onUpdateRole_daoError() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    when(roleDao.findByName("ORGANIZER")).thenThrow(new RuntimeException("Update failed"));

    var testKit = BehaviorTestKit.create(createBehavior(roleDao));
    TestInbox<StudioRoleAdminResponse> inbox = TestInbox.create();

    testKit.run(
        new StudioRoleAdminCommand.UpdateRole(
            roleId, "ORGANIZER", Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioRoleAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("DefaultStudioRoleAdminActorFactory creates behavior successfully")
  void factory_createsBehavior() {
    var roleDao = mock(DefaultStudioRoleDAO.class);
    var factory = new DefaultStudioRoleAdminActorFactory(roleDao);
    assertThat(factory.create()).isNotNull();
  }
}
