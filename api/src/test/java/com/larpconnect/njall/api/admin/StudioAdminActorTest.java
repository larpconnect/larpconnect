package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.StudioDAO;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StudioAdminActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<StudioAdminCommand> createBehavior(StudioDAO studioDao) {
    return Behaviors.setup(context -> new StudioAdminActor(context, studioDao));
  }

  private StudioLookup sampleStudio() {
    return StudioLookup.of(tenantId, studioId, "valhalla", now, now, null);
  }

  @Test
  @DisplayName("onCreateStudio creates studio when alias is valid and unique")
  void onCreateStudio_success() {
    var studioDao = mock(StudioDAO.class);
    var studio = sampleStudio();
    when(studioDao.findByAlias("valhalla", true)).thenReturn(Optional.empty());
    when(studioDao.create("valhalla")).thenReturn(studio);

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.CreateStudio("valhalla", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.StudioSingle.class);
    assertThat(((StudioAdminResponse.StudioSingle) response).studio().alias())
        .isEqualTo("valhalla");
  }

  @Test
  @DisplayName("onCreateStudio rejects invalid alias format")
  void onCreateStudio_invalidAlias_rejects() {
    var studioDao = mock(StudioDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.CreateStudio("Invalid-Alias", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onCreateStudio rejects duplicate alias with conflict")
  void onCreateStudio_duplicateAlias_conflict() {
    var studioDao = mock(StudioDAO.class);
    when(studioDao.findByAlias("valhalla", true)).thenReturn(Optional.of(sampleStudio()));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.CreateStudio("valhalla", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.Conflict.class);
  }

  @Test
  @DisplayName("onCreateStudio replies with error when DAO throws exception")
  void onCreateStudio_daoError() {
    var studioDao = mock(StudioDAO.class);
    when(studioDao.findByAlias(anyString(), anyBoolean()))
        .thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.CreateStudio("valhalla", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onListStudios returns studio list")
  void onListStudios_success() {
    var studioDao = mock(StudioDAO.class);
    var studio = sampleStudio();
    when(studioDao.list(false)).thenReturn(ImmutableList.of(studio));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.ListStudios(false, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.StudioList.class);
    assertThat(((StudioAdminResponse.StudioList) response).studios()).containsExactly(studio);
  }

  @Test
  @DisplayName("onListStudios replies with error on DAO failure")
  void onListStudios_daoError() {
    var studioDao = mock(StudioDAO.class);
    when(studioDao.list(anyBoolean())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.ListStudios(false, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onGetStudioById returns single when found and notFound when missing")
  void onGetStudioById_foundAndNotFound() {
    var studioDao = mock(StudioDAO.class);
    var studio = sampleStudio();
    when(studioDao.findById(studioId, false)).thenReturn(Optional.of(studio));
    var missingId = UUID.randomUUID();
    when(studioDao.findById(missingId, false)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.GetStudioById(studioId, false, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(StudioAdminResponse.StudioSingle.class);

    testKit.run(new StudioAdminCommand.GetStudioById(missingId, false, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(StudioAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetStudioById replies with error on DAO failure")
  void onGetStudioById_daoError() {
    var studioDao = mock(StudioDAO.class);
    when(studioDao.findById(any(), anyBoolean())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.GetStudioById(studioId, false, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(StudioAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("onGetStudioByAlias returns single when found and notFound when missing")
  void onGetStudioByAlias_foundAndNotFound() {
    var studioDao = mock(StudioDAO.class);
    var studio = sampleStudio();
    when(studioDao.findByAlias("valhalla", false)).thenReturn(Optional.of(studio));
    when(studioDao.findByAlias("unknown", false)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.GetStudioByAlias("valhalla", false, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(StudioAdminResponse.StudioSingle.class);

    testKit.run(new StudioAdminCommand.GetStudioByAlias("unknown", false, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(StudioAdminResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetStudioByAlias replies with error on DAO failure")
  void onGetStudioByAlias_daoError() {
    var studioDao = mock(StudioDAO.class);
    when(studioDao.findByAlias(anyString(), anyBoolean()))
        .thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.GetStudioByAlias("valhalla", false, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(StudioAdminResponse.Failure.class);
  }

  @Test
  @DisplayName("DefaultStudioAdminActorFactory creates behavior successfully")
  void factory_createsBehavior() {
    var studioDao = mock(StudioDAO.class);
    var factory = new DefaultStudioAdminActorFactory(studioDao);
    assertThat(factory.create()).isNotNull();
  }

  @Test
  @DisplayName("handleError falls back to default reason when exception message is null")
  void onCreateStudio_daoErrorWithNullMessage() {
    var studioDao = mock(StudioDAO.class);
    when(studioDao.findByAlias(anyString(), anyBoolean())).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioAdminResponse> inbox = TestInbox.create();

    testKit.run(new StudioAdminCommand.CreateStudio("valhalla", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioAdminResponse.Failure.class);
    assertThat(((StudioAdminResponse.Failure) response).message())
        .contains("Error executing create studio");
  }
}
