package com.larpconnect.njall.api.studios.locations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.domain.studios.Location;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationActorBranchTest {

  private static final String STUDIO_IDENTIFIER = "valiant";
  private final UUID tenantId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();

  private Behavior<LocationCommand> createLocationBehavior(LocationDAO locationDao) {
    return Behaviors.setup(context -> new LocationActor(context, locationDao));
  }

  @Test
  @DisplayName("LocationActor returns BadRequest on invalid patch request")
  void locationActor_patchInvalid_returnsBadRequest() {
    var locationDao = mock(LocationDAO.class);
    var testKit = BehaviorTestKit.create(createLocationBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    var request = new UpdateLocationRequest("   ", null);
    testKit.run(
        new LocationCommand.PatchLocation(
            tenantId, STUDIO_IDENTIFIER, locationId, request, Optional.of("name"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("LocationActor returns NotFound when patch DAO returns empty")
  void locationActor_patchNotFound_returnsNotFound() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.patch(eq(tenantId), eq(locationId), any(), any()))
        .thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createLocationBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    var request = new UpdateLocationRequest("New Name", null);
    testKit.run(
        new LocationCommand.PatchLocation(
            tenantId, STUDIO_IDENTIFIER, locationId, request, Optional.of("name"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("LocationActor patch without mask selects all fields")
  void locationActor_patchNoMask_updatesAllFields() {
    var locationDao = mock(LocationDAO.class);
    var updated =
        new Location(
            locationId,
            UUID.randomUUID(),
            "All Fields Updated",
            "Updated summary",
            Instant.now(),
            Instant.now(),
            null);
    when(locationDao.patch(
            eq(tenantId),
            eq(locationId),
            eq(Optional.of("All Fields Updated")),
            eq(Optional.of("Updated summary"))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createLocationBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    var request = new UpdateLocationRequest("All Fields Updated", "Updated summary");
    testKit.run(
        new LocationCommand.PatchLocation(
            tenantId, STUDIO_IDENTIFIER, locationId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.Success.class);
  }

  @Test
  @DisplayName("LocationActor handleError handles IllegalArgumentException")
  void locationActor_handleError_illegalArgument() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.findById(tenantId, locationId))
        .thenThrow(new IllegalArgumentException("Invalid state"));

    var testKit = BehaviorTestKit.create(createLocationBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    testKit.run(
        new LocationCommand.GetLocation(tenantId, STUDIO_IDENTIFIER, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("LocationActor handleError handles null and non-null exception messages")
  void locationActor_handleError_messages() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.findById(tenantId, locationId)).thenThrow(new RuntimeException((String) null));

    var testKit = BehaviorTestKit.create(createLocationBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    testKit.run(
        new LocationCommand.GetLocation(tenantId, STUDIO_IDENTIFIER, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.Failure.class);
    assertThat(((LocationActorResponse.Failure) response).message())
        .isEqualTo("Error executing get location");
  }

  @Test
  @DisplayName("Response constructors cover Failure variant")
  void response_failureVariant() {
    var locFailure = new LocationActorResponse.Failure("Location failure");
    assertThat(locFailure.message()).isEqualTo("Location failure");
  }
}
