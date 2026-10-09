package com.larpconnect.njall.api.studios.addresses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AddressActorBranchTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final UUID addressId = UUID.randomUUID();

  private Behavior<AddressCommand> createAddressBehavior(AddressDAO addressDao) {
    return Behaviors.setup(context -> new AddressActor(context, addressDao));
  }

  @Test
  @DisplayName("AddressActor returns BadRequest on invalid create request")
  void addressActor_createInvalid_returnsBadRequest() {
    var addressDao = mock(AddressDAO.class);
    var testKit = BehaviorTestKit.create(createAddressBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    var request =
        new CreateAddressRequest(AddressType.PHYSICAL, "", "Seattle", "WA", "98101", "US");
    testKit.run(new AddressCommand.CreateAddress(tenantId, locationId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("AddressActor returns NotFound when get address not found in DAO")
  void addressActor_getNotFound_returnsNotFound() {
    var addressDao = mock(AddressDAO.class);
    when(addressDao.findById(tenantId, locationId, addressId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createAddressBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    testKit.run(new AddressCommand.GetAddress(tenantId, locationId, addressId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("AddressActor returns BadRequest on invalid patch request")
  void addressActor_patchInvalid_returnsBadRequest() {
    var addressDao = mock(AddressDAO.class);
    var testKit = BehaviorTestKit.create(createAddressBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    var request =
        new UpdateAddressRequest(
            Optional.empty(),
            Optional.of(""),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    testKit.run(
        new AddressCommand.PatchAddress(
            tenantId, locationId, addressId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("AddressActor returns NotFound when patch DAO returns empty")
  void addressActor_patchNotFound_returnsNotFound() {
    var addressDao = mock(AddressDAO.class);
    var patchBuilder = mock(AddressDAO.PatchBuilder.class, RETURNS_SELF);
    when(addressDao.patch(tenantId, locationId, addressId)).thenReturn(patchBuilder);
    when(patchBuilder.execute()).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createAddressBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    var request = new UpdateAddressRequest();
    testKit.run(
        new AddressCommand.PatchAddress(
            tenantId, locationId, addressId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("AddressActor returns NotFound when delete returns false")
  void addressActor_deleteNotFound_returnsNotFound() {
    var addressDao = mock(AddressDAO.class);
    when(addressDao.delete(tenantId, locationId, addressId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createAddressBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    testKit.run(new AddressCommand.DeleteAddress(tenantId, locationId, addressId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("Response and request constructors cover DTO variants")
  void responseAndRequest_dtoVariants() {
    var addrFailure = new AddressActorResponse.Failure("Address failure");
    assertThat(addrFailure.message()).isEqualTo("Address failure");

    var addrBadRequest = new AddressActorResponse.BadRequest("Invalid field");
    assertThat(addrBadRequest.message()).isEqualTo("Invalid field");

    var point = new GeoJsonPoint(-122.33, 47.60);
    var reqWithPoint =
        new CreateAddressRequest(
            AddressType.PHYSICAL,
            "123 Pine",
            "Suite 1",
            "Bldg A",
            "Seattle",
            "WA",
            "98101",
            "US",
            point);
    assertThat(reqWithPoint.geom()).contains(point);
    assertThat(reqWithPoint.addressLine2()).contains("Suite 1");
    assertThat(reqWithPoint.addressLine3()).contains("Bldg A");

    var reqWithBlankLines =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "123 Pine", "   ", "", "Seattle", "WA", "98101", "US", null);
    assertThat(reqWithBlankLines.geom()).isEmpty();
    assertThat(reqWithBlankLines.addressLine2()).isEmpty();
    assertThat(reqWithBlankLines.addressLine3()).isEmpty();

    var reqWithoutPoint =
        new CreateAddressRequest(AddressType.PHYSICAL, "123 Pine", "Seattle", "WA", "98101", "US");
    assertThat(reqWithoutPoint.geom()).isEmpty();
  }
}
