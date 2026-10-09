package com.larpconnect.njall.api.studios.locations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.api.studios.addresses.*;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.domain.Location;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<LocationCommand> createBehavior(LocationDAO locationDao) {
    return Behaviors.setup(context -> new LocationActor(context, locationDao));
  }

  private Location sampleLocation() {
    return new Location(locationId, "Camp Whispering Pines", "Main campsite", now, now, null);
  }

  @Test
  @DisplayName("onCreateLocation returns Success on valid payload")
  void onCreateLocation_valid_returnsSuccess() {
    var locationDao = mock(LocationDAO.class);
    var domain = sampleLocation();
    when(locationDao.create(
            eq(tenantId), eq("Camp Whispering Pines"), eq(Optional.of("Main campsite"))))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    var request = new CreateLocationRequest("Camp Whispering Pines", "Main campsite");
    testKit.run(new LocationCommand.CreateLocation(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.Success.class);
    var success = (LocationActorResponse.Success) response;
    assertThat(success.location().id()).isEqualTo(locationId);
    assertThat(success.location().name()).isEqualTo("Camp Whispering Pines");
  }

  @Test
  @DisplayName("onCreateLocation returns BadRequest on blank name")
  void onCreateLocation_blankName_returnsBadRequest() {
    var locationDao = mock(LocationDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    var request = new CreateLocationRequest("");
    testKit.run(new LocationCommand.CreateLocation(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onGetLocation returns Success when found")
  void onGetLocation_found_returnsSuccess() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.findById(tenantId, locationId)).thenReturn(Optional.of(sampleLocation()));

    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    testKit.run(new LocationCommand.GetLocation(tenantId, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.Success.class);
    var success = (LocationActorResponse.Success) response;
    assertThat(success.location().id()).isEqualTo(locationId);
  }

  @Test
  @DisplayName("onGetLocation returns NotFound when not found")
  void onGetLocation_notFound_returnsNotFound() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.findById(tenantId, locationId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    testKit.run(new LocationCommand.GetLocation(tenantId, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onPatchLocation returns Success on valid update")
  void onPatchLocation_valid_returnsSuccess() {
    var locationDao = mock(LocationDAO.class);
    var updated = new Location(locationId, "Camp North", "Updated camp", now, now, null);
    when(locationDao.patch(
            eq(tenantId), eq(locationId), eq(Optional.of("Camp North")), eq(Optional.empty())))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    var request = new UpdateLocationRequest("Camp North", null);
    testKit.run(
        new LocationCommand.PatchLocation(
            tenantId, locationId, request, Optional.of("name"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.Success.class);
    var success = (LocationActorResponse.Success) response;
    assertThat(success.location().name()).isEqualTo("Camp North");
  }

  @Test
  @DisplayName("onDeleteLocation returns Deleted when softDelete succeeds")
  void onDeleteLocation_success_returnsDeleted() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.softDelete(tenantId, locationId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    testKit.run(new LocationCommand.DeleteLocation(tenantId, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.Deleted.class);
  }

  @Test
  @DisplayName("onDeleteLocation returns NotFound when not found")
  void onDeleteLocation_notFound_returnsNotFound() {
    var locationDao = mock(LocationDAO.class);
    when(locationDao.softDelete(tenantId, locationId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(locationDao));
    TestInbox<LocationActorResponse> inbox = TestInbox.create();

    testKit.run(new LocationCommand.DeleteLocation(tenantId, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LocationActorResponse.NotFound.class);
  }
}
