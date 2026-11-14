package com.larpconnect.njall.api.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.events.EventDAO;
import com.larpconnect.njall.data.dao.studios.events.PatchEventParams;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Event;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EventActorTest {

  private static final String STUDIO_IDENTIFIER = "valiant";

  private final UUID tenantId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<EventCommand> createBehavior(EventDAO eventDao) {
    return EventActor.create(eventDao);
  }

  private Event sampleEvent() {
    return new Event(
        eventId,
        ownerId,
        EventType.EVENT_TYPE_UNSPECIFIED,
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
            eq(ownerId),
            eq(EventType.EVENT_TYPE_UNSPECIFIED),
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
            "/api/studios/valiant/v1/actors/" + ownerId,
            "Autumn Harvest Festival",
            "Community gathering",
            locationId,
            now,
            now.plusSeconds(3600));
    testKit.run(new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Success.class);
    var success = (EventActorResponse.Success) response;
    assertThat(success.event().id()).isEqualTo(eventId);
    assertThat(success.event().title()).isEqualTo("Autumn Harvest Festival");
    assertThat(success.event().eventType()).isEqualTo(EventType.EVENT_TYPE_UNSPECIFIED);
    assertThat(success.event().generator()).isEqualTo("/api/studios/valiant/v1/actors/" + ownerId);
  }

  @Test
  @DisplayName("onCreateEvent accepts explicit non-GAME eventType and UUID studio URI via lookup")
  void onCreateEvent_explicitEventTypeAndLookup_returnsSuccess() {
    var eventDao = mock(EventDAO.class);
    var domain =
        new Event(
            eventId,
            ownerId,
            EventType.CONFERENCE,
            Optional.empty(),
            "Larp Summit",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            now,
            now,
            Optional.empty());
    when(eventDao.create(
            eq(tenantId),
            eq(ownerId),
            eq(EventType.CONFERENCE),
            eq("Larp Summit"),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty())))
        .thenReturn(domain);

    var studioId = UUID.randomUUID();
    var lookup = new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request =
        new CreateEventRequest(
            "/api/studios/" + studioId + "/v1/actors/" + ownerId,
            "Larp Summit",
            Optional.empty(),
            Optional.of(EventType.CONFERENCE),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    testKit.run(
        new EventCommand.CreateEvent(
            tenantId, STUDIO_IDENTIFIER, request, inbox.getRef(), Optional.of(lookup)));

    var response = (EventActorResponse.Success) inbox.receiveMessage();
    assertThat(response.event().eventType()).isEqualTo(EventType.CONFERENCE);
  }

  @Test
  @DisplayName("onCreateEvent rejects GAME eventType and wrong-collection generator URIs")
  void onCreateEvent_gameTypeOrWrongCollection_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var gameReq =
        new CreateEventRequest(
            ownerId.toString(),
            "Sunday Game",
            Optional.empty(),
            Optional.of(EventType.GAME),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    testKit.run(new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, gameReq, inbox.getRef()));
    assertThat(((EventActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    var wrongColReq =
        new CreateEventRequest("/api/studios/valiant/v1/systems/" + ownerId, "Autumn Festival");
    testKit.run(
        new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, wrongColReq, inbox.getRef()));
    assertThat(((EventActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateEvent maps IllegalArgumentException without message to BadRequest")
  void onCreateEvent_illegalArgumentNullMessage_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException());

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();
    testKit.run(
        new EventCommand.CreateEvent(
            tenantId,
            STUDIO_IDENTIFIER,
            new CreateEventRequest(ownerId.toString(), "Autumn Festival"),
            inbox.getRef()));

    var failure = (EventActorResponse.Failure) inbox.receiveMessage();
    assertThat(failure.status()).isEqualTo(400);
    assertThat(failure.message()).isEqualTo("Invalid request");
  }

  @Test
  @DisplayName("onCreateEvent returns BadRequest on blank title")
  void onCreateEvent_blankTitle_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest(ownerId.toString(), "");
    testKit.run(new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateEvent returns BadRequest on invalid generator")
  void onCreateEvent_invalidGenerator_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest("invalid-generator", "Autumn Festival");
    testKit.run(new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateEvent maps constraint violations to BadRequest")
  void onCreateEvent_constraintViolation_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var cve =
        new ConstraintViolationException(
            "violates foreign key constraint", new SQLException("fk"), "fk_events_locations");
    when(eventDao.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("outer", cve));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest(ownerId.toString(), "Autumn Festival");
    testKit.run(new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateEvent maps other errors to Failure")
  void onCreateEvent_genericError_returnsFailure() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Database timeout"));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new CreateEventRequest(ownerId.toString(), "Autumn Festival");
    testKit.run(new EventCommand.CreateEvent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

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

    testKit.run(new EventCommand.GetEvent(tenantId, STUDIO_IDENTIFIER, eventId, inbox.getRef()));

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

    testKit.run(new EventCommand.GetEvent(tenantId, STUDIO_IDENTIFIER, eventId, inbox.getRef()));

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

    testKit.run(new EventCommand.ListEvents(tenantId, STUDIO_IDENTIFIER, inbox.getRef()));

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
            eq(
                new PatchEventParams(
                    Optional.empty(),
                    Optional.of("Updated Title"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new UpdateEventRequest("Updated Title", null, null, null, null);
    testKit.run(
        new EventCommand.PatchEvent(
            tenantId, STUDIO_IDENTIFIER, eventId, request, Optional.of("title"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchEvent supports event_type mask and explicit clearing of nullable fields")
  void onPatchEvent_updateMaskClearsNullableFields_populatesClearFlags() {
    var eventDao = mock(EventDAO.class);
    var expectedParams =
        new PatchEventParams(
            Optional.of(EventType.MEETUP),
            Optional.of("Updated Title"),
            Optional.empty(),
            true,
            Optional.empty(),
            true,
            Optional.empty(),
            true,
            Optional.empty(),
            true);
    when(eventDao.patch(eq(tenantId), eq(eventId), eq(expectedParams)))
        .thenReturn(Optional.of(sampleEvent()));

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();
    var request =
        new UpdateEventRequest(
            Optional.of("Updated Title"),
            Optional.empty(),
            Optional.of(EventType.MEETUP),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    testKit.run(
        new EventCommand.PatchEvent(
            tenantId,
            STUDIO_IDENTIFIER,
            eventId,
            request,
            Optional.of("title,event_type,summary,location_id,start_time,end_time"),
            inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(EventActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchEvent rejects update_mask attempting to clear required fields")
  void onPatchEvent_clearingRequiredFieldViaMask_returnsBadRequest() {
    var eventDao = mock(EventDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    testKit.run(
        new EventCommand.PatchEvent(
            tenantId,
            STUDIO_IDENTIFIER,
            eventId,
            new UpdateEventRequest(),
            Optional.of("title"),
            inbox.getRef()));
    assertThat(((EventActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new EventCommand.PatchEvent(
            tenantId,
            STUDIO_IDENTIFIER,
            eventId,
            new UpdateEventRequest(),
            Optional.of("event_type"),
            inbox.getRef()));
    assertThat(((EventActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
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
            tenantId, STUDIO_IDENTIFIER, eventId, request, Optional.of("title"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(EventActorResponse.Failure.class);
    assertThat(((EventActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchEvent returns NotFound when event not found")
  void onPatchEvent_notFound_returnsNotFound() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.patch(any(), any(), any())).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(eventDao));
    TestInbox<EventActorResponse> inbox = TestInbox.create();

    var request = new UpdateEventRequest("New Title", null, null, null, null);
    testKit.run(
        new EventCommand.PatchEvent(
            tenantId, STUDIO_IDENTIFIER, eventId, request, Optional.empty(), inbox.getRef()));

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
  @DisplayName("DefaultEventActorFactory creates runnable EventActor behavior")
  void defaultFactory_executesCommand() {
    var eventDao = mock(EventDAO.class);
    when(eventDao.softDelete(tenantId, eventId)).thenReturn(true);
    var factory = new DefaultEventActorFactory(eventDao);
    var testKit = BehaviorTestKit.create(factory.create());
    var inbox = TestInbox.<EventActorResponse>create();

    testKit.run(new EventCommand.DeleteEvent(tenantId, eventId, inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(EventActorResponse.Deleted.class);
  }
}
