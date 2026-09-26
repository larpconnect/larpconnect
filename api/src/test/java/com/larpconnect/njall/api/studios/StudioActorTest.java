package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.StudioDAO;
import com.larpconnect.njall.data.dao.StudioLookupDAO;
import com.larpconnect.njall.data.domain.DeletionFilter;
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

  private Behavior<StudioCommand> createBehavior(
      StudioLookupDAO studioLookupDao, StudioDAO studioDao) {
    return Behaviors.setup(context -> new StudioActor(context, studioLookupDao, studioDao));
  }

  @Test
  @DisplayName("onGetStudio returns Success when studio is found by alias")
  void onGetStudio_foundByAlias_returnsSuccess() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    var studio = new Studio(tenantId, "Valiant Games");
    when(studioLookupDao.findByAlias("valiant", DeletionFilter.ACTIVE_ONLY))
        .thenReturn(Optional.of(lookup));
    when(studioDao.findById(tenantId)).thenReturn(Optional.of(studio));

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio("valiant", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Success.class);
    var success = (StudioActorResponse.Success) response;
    assertThat(success.studio().studioId()).isEqualTo(studioId);
    assertThat(success.studio().alias()).isEqualTo("valiant");
    assertThat(success.studio().name()).isEqualTo("Valiant Games");
  }

  @Test
  @DisplayName("onGetStudio returns Success when studio is found by UUID")
  void onGetStudio_foundByUuid_returnsSuccess() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    var studio = new Studio(tenantId, "Valiant Games");
    when(studioLookupDao.findById(studioId, DeletionFilter.ACTIVE_ONLY))
        .thenReturn(Optional.of(lookup));
    when(studioDao.findById(tenantId)).thenReturn(Optional.of(studio));

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio(studioId.toString(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Success.class);
    var success = (StudioActorResponse.Success) response;
    assertThat(success.studio().studioId()).isEqualTo(studioId);
    assertThat(success.studio().alias()).isEqualTo("valiant");
    assertThat(success.studio().name()).isEqualTo("Valiant Games");
  }

  @Test
  @DisplayName("onGetStudio returns NotFound when lookup is missing")
  void onGetStudio_lookupMissing_returnsNotFound() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    when(studioLookupDao.findByAlias("missing", DeletionFilter.ACTIVE_ONLY))
        .thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio("missing", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetStudio returns NotFound when studio is missing in tenant")
  void onGetStudio_studioMissingInTenant_returnsNotFound() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    var lookup = sampleLookup();
    when(studioLookupDao.findByAlias("valiant", DeletionFilter.ACTIVE_ONLY))
        .thenReturn(Optional.of(lookup));
    when(studioDao.findById(tenantId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio("valiant", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetStudio returns Failure when DAO throws exception with message")
  void onGetStudio_daoThrows_returnsFailure() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    when(studioLookupDao.findByAlias("valiant", DeletionFilter.ACTIVE_ONLY))
        .thenThrow(new RuntimeException("Database down"));

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio("valiant", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Failure.class);
    assertThat(((StudioActorResponse.Failure) response).message()).isEqualTo("Database down");
  }

  @Test
  @DisplayName("onGetStudio returns Failure when DAO throws exception with null message")
  void onGetStudio_daoThrowsNullMessage_returnsFailure() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    when(studioLookupDao.findByAlias("valiant", DeletionFilter.ACTIVE_ONLY))
        .thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio("valiant", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Failure.class);
    assertThat(((StudioActorResponse.Failure) response).message())
        .contains("Error executing get studio");
  }

  @Test
  @DisplayName("onGetStudio returns Failure when DAO throws exception with empty message")
  void onGetStudio_daoThrowsEmptyMessage_returnsFailure() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    when(studioLookupDao.findByAlias("valiant", DeletionFilter.ACTIVE_ONLY))
        .thenThrow(new RuntimeException(""));

    var testKit = BehaviorTestKit.create(createBehavior(studioLookupDao, studioDao));
    TestInbox<StudioActorResponse> inbox = TestInbox.create();

    testKit.run(new StudioCommand.GetStudio("valiant", inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(StudioActorResponse.Failure.class);
    assertThat(((StudioActorResponse.Failure) response).message())
        .contains("Error executing get studio");
  }

  @Test
  @DisplayName("DefaultStudioActorFactory creates behavior successfully")
  void factory_createsBehavior() {
    var studioLookupDao = mock(StudioLookupDAO.class);
    var studioDao = mock(StudioDAO.class);
    var factory = new DefaultStudioActorFactory(studioLookupDao, studioDao);
    assertThat(factory.create()).isInstanceOf(Behavior.class);
  }
}
