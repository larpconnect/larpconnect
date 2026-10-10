package com.larpconnect.njall.api.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.EventDAO;
import com.larpconnect.njall.data.domain.Event;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EventActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<EventCommand> createBehavior(EventDAO eventDao) {
    return Behaviors.setup(context -> new EventActor(context, eventDao));
  }

  private Event sampleEvent() {
    return new Event(
        eventId,
        locationId,
        "Autumn Harvest Festival",
        "Community gathering",
        now,
        now.plusSeconds(3600),
        now,
        now,
        null);
  }

  @Test
  @DisplayName("onCreateEvent returns Success on valid payload")
  void onCreateEvent_valid_returnsSuccess() {
    var eventDao = mock(EventDAO.class);
    var domain = sampleEvent();
    when(eventDao.create(
            eq(tenantId),
            eq("Autumn Harvest Festival"),
            eq(Optional.of("Community gathering")),
            eq(Optional.of(locationId)),
            eq(Optional.of(now)),
            eq(Optional.of(now.plusSeconds(3600)))))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request =
        new CreateEventRequest(
            "Autumn Harvest Festival",
            "Community gathering",
            locationId,
            now,
            now.plusSeconds(3600));
    testKit.run(new EventCommand.CreateEvent(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Success.class);
    var success = (EventActorResponse.Success) response;
    assertThat(success.event().id()).isEqualTo(eventId);
    assertThat(success.event().title()).isEqualTo("Autumn Harvest Festival");
  }

  @Test
  @DisplayName("onCreateEvent returns BadRequest on blank title")
  void onCreateEvent_blankTitle_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest("");
    testKit.run(new EventCommand.CreateEvent(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateEvent maps constraint violations to BadRequest")
  void onCreateEvent_constraintViolation_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.create(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("violates foreign key constraint fk_events_locations"));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest("Autumn Festival");
    testKit.run(new EventCommand.CreateEvent(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateEvent maps other errors to Failure")
  void onCreateEvent_genericError_returnsFailure() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.create(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Database timeout"));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest("Autumn Festival");
    testKit.run(new EventCommand.CreateEvent(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
  }

  @Test
  @DisplayName("onGetEvent returns Success when found")
  void onGetEvent_found_returnsSuccess() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.findById(tenantId, eventId)).thenReturn(Optional.of(sampleEvent()));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    testKit.run(new EventCommand.GetEvent(tenantId, eventId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Success.class);
    var success = (EventActorResponse.Success) response;
    assertThat(success.event().id()).isEqualTo(eventId);
  }

  @Test
  @DisplayName("onGetEvent returns NotFound when missing")
  void onGetEvent_notFound_returnsNotFound() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.findById(tenantId, eventId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    testKit.run(new EventCommand.GetEvent(tenantId, eventId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onListEvents returns Items with all events")
  void onListEvents_returnsItems() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.listAll(tenantId)).thenReturn(ImmutableList.of(sampleEvent()));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    testKit.run(new EventCommand.ListEvents(tenantId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Items.class);
    var items = (EventActorResponse.Items) response;
    assertThat(items.events()).hasSize(1);
    assertThat(items.events().get(0).title()).isEqualTo("Autumn Harvest Festival");
  }

  @Test
  @DisplayName("onPatchEvent returns Success on valid update")
  void onPatchEvent_valid_returnsSuccess() {
    var eventDao = mock(EventDAO.class);
    var updated = sampleEvent();
    when(eventDao.patch(
            eq(tenantId),
            eq(eventId),
            eq(Optional.of("Updated Title")),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty())))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new UpdateEventRequest("Updated Title", null, null, null, null);
    testKit.run(
        new EventCommand.PatchEvent(
            tenantId, eventId, request, Optional.of("title"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchEvent returns BadRequest on blank title")
  void onPatchEvent_invalid_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new UpdateEventRequest("   ", null, null, null, null);
    testKit.run(
        new EventCommand.PatchEvent(
            tenantId, eventId, request, Optional.of("title"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchEvent returns NotFound when event not found")
  void onPatchEvent_notFound_returnsNotFound() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.patch(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new UpdateEventRequest("New Title", null, null, null, null);
    testKit.run(
        new EventCommand.PatchEvent(tenantId, eventId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onDeleteEvent returns Deleted on success")
  void onDeleteEvent_success_returnsDeleted() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.softDelete(tenantId, eventId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    testKit.run(new EventCommand.DeleteEvent(tenantId, eventId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Deleted.class);
  }

  @Test
  @DisplayName("onDeleteEvent returns NotFound when not found")
  void onDeleteEvent_notFound_returnsNotFound() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.softDelete(tenantId, eventId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    testKit.run(new EventCommand.DeleteEvent(tenantId, eventId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("DefaultEventActorFactory creates behavior successfully")
  void defaultFactory_createsBehavior() {
    var eventDao = mock(EventDAO.class);
    var factory = new DefaultEventActorFactory(eventDao);
    assertThat(factory.create()).isNotNull();
  }
}
