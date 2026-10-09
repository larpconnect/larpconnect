package com.larpconnect.njall.api.studios.locations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.LocationDAO;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationActorBranchTest {

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
            tenantId, locationId, request, Optional.of("name"), inbox.getRef()));

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
            tenantId, locationId, request, Optional.of("name"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("Response constructors cover Failure variant")
  void response_failureVariant() {
    var locFailure = new LocationActorResponse.Failure("Location failure");
    assertThat(locFailure.message()).isEqualTo("Location failure");
  }
}
