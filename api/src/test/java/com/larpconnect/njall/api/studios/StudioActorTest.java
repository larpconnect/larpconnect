package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.domain.Studio;
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

final class StudioActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private StudioLookup sampleLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private Behavior<StudioCommand> createBehavior(StudioDAO studioDao) {
    return Behaviors.setup(context -> new StudioActor(context, studioDao));
  }

  @Test
  @DisplayName("onGetStudio returns Success when studio is found in tenant database")
  void onGetStudio_found_returnsSuccess() {
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    var studio = new Studio(tenantId, "Valiant Games");
    when(studioDao.findById(tenantId)).thenReturn(Optional.of(studio));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio(lookup, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Success.class);
    var success = (StudioActorResponse.Success) response;
    assertThat(success.studio().studioId()).isEqualTo(studioId);
    assertThat(success.studio().alias()).isEqualTo("valiant");
    assertThat(success.studio().name()).isEqualTo("Valiant Games");
  }

  @Test
  @DisplayName("onGetStudio returns NotFound when studio is missing in tenant database")
  void onGetStudio_studioMissingInTenant_returnsNotFound() {
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    when(studioDao.findById(tenantId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio(lookup, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetStudio returns Failure when DAO throws exception with message")
  void onGetStudio_daoThrows_returnsFailure() {
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    when(studioDao.findById(tenantId)).thenThrow(new RuntimeException("Database down"));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio(lookup, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Failure.class);
    assertThat(((StudioActorResponse.Failure) response).message()).isEqualTo("Database down");
  }

  @Test
  @DisplayName("onGetStudio returns Failure when DAO throws exception with null message")
  void onGetStudio_daoThrowsNullMessage_returnsFailure() {
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    when(studioDao.findById(tenantId)).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio(lookup, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Failure.class);
    assertThat(((StudioActorResponse.Failure) response).message())
        .contains("Error executing get studio");
  }

  @Test
  @DisplayName("onGetStudio returns Failure when DAO throws exception with empty message")
  void onGetStudio_daoThrowsEmptyMessage_returnsFailure() {
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    when(studioDao.findById(tenantId)).thenThrow(new RuntimeException(""));

    var testKit = BehaviorTestKit.create(createBehavior(studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio(lookup, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Failure.class);
    assertThat(((StudioActorResponse.Failure) response).message())
        .contains("Error executing get studio");
  }

  @Test
  @DisplayName("DefaultStudioActorFactory creates behavior successfully")
  void factory_createsBehavior() {
    var studioDao = mock(StudioDAO.class);
    var factory = new DefaultStudioActorFactory(studioDao);
    assertThat(factory.create()).isInstanceOf(Behavior.class);
  }
}
