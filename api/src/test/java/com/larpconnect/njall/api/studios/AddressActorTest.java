package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.AddressDAO;
import com.larpconnect.njall.data.domain.Address;
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

final class AddressActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final UUID addressId = UUID.randomUUID();

  private Behavior<AddressCommand> createBehavior(AddressDAO addressDao) {
    return Behaviors.setup(context -> new AddressActor(context, addressDao));
  }

  private Address sampleAddress() {
    return new Address(
        addressId,
        locationId,
        AddressType.PHYSICAL,
        "123 Pine St",
        "Suite A",
        null,
        "Seattle",
        "WA",
        "98101",
        "US",
        new GeoJsonPoint(-122.3321, 47.6062));
  }

  @Test
  @DisplayName("onCreateAddress returns Success on valid request")
  void onCreateAddress_valid_returnsSuccess() {
    var addressDao = mock(AddressDAO.class);
    var createBuilder = mock(AddressDAO.CreateBuilder.class, RETURNS_SELF);
    when(addressDao.create(tenantId, locationId)).thenReturn(createBuilder);
    when(createBuilder.execute()).thenReturn(sampleAddress());

    var testKit = BehaviorTestKit.create(createBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    var request =
        new CreateAddressRequest(
            AddressType.PHYSICAL,
            "123 Pine St",
            "Suite A",
            null,
            "Seattle",
            "WA",
            "98101",
            "US",
            new GeoJsonPoint(-122.3321, 47.6062));
    testKit.run(new AddressCommand.CreateAddress(tenantId, locationId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.Success.class);
    var success = (AddressActorResponse.Success) response;
    assertThat(success.address().id()).isEqualTo(addressId);
    assertThat(success.address().addressType()).isEqualTo(AddressType.PHYSICAL);
  }

  @Test
  @DisplayName("onCreateAddress returns NotFound when parent location is inactive")
  void onCreateAddress_inactiveLocation_returnsNotFound() {
    var addressDao = mock(AddressDAO.class);
    var createBuilder = mock(AddressDAO.CreateBuilder.class, RETURNS_SELF);
    when(addressDao.create(tenantId, locationId)).thenReturn(createBuilder);
    when(createBuilder.execute())
        .thenThrow(new IllegalArgumentException("Parent location not found or inactive"));

    var testKit = BehaviorTestKit.create(createBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    var request =
        new CreateAddressRequest(
            AddressType.PHYSICAL, "123 Pine St", "Seattle", "WA", "98101", "US");
    testKit.run(new AddressCommand.CreateAddress(tenantId, locationId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetAddress returns Success when found")
  void onGetAddress_found_returnsSuccess() {
    var addressDao = mock(AddressDAO.class);
    when(addressDao.findById(tenantId, locationId, addressId))
        .thenReturn(Optional.of(sampleAddress()));

    var testKit = BehaviorTestKit.create(createBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    testKit.run(new AddressCommand.GetAddress(tenantId, locationId, addressId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.Success.class);
  }

  @Test
  @DisplayName("onListAddresses returns ListSuccess")
  void onListAddresses_returnsListSuccess() {
    var addressDao = mock(AddressDAO.class);
    when(addressDao.listByLocation(tenantId, locationId))
        .thenReturn(ImmutableList.of(sampleAddress()));

    var testKit = BehaviorTestKit.create(createBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    testKit.run(new AddressCommand.ListAddresses(tenantId, locationId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.ListSuccess.class);
    var list = (AddressActorResponse.ListSuccess) response;
    assertThat(list.addresses()).hasSize(1);
  }

  @Test
  @DisplayName("onPatchAddress returns Success on valid update")
  void onPatchAddress_valid_returnsSuccess() {
    var addressDao = mock(AddressDAO.class);
    var patchBuilder = mock(AddressDAO.PatchBuilder.class, RETURNS_SELF);
    when(addressDao.patch(tenantId, locationId, addressId)).thenReturn(patchBuilder);
    when(patchBuilder.execute()).thenReturn(Optional.of(sampleAddress()));

    var testKit = BehaviorTestKit.create(createBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    var request = new UpdateAddressRequest();
    testKit.run(
        new AddressCommand.PatchAddress(
            tenantId, locationId, addressId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.Success.class);
  }

  @Test
  @DisplayName("onDeleteAddress returns Deleted when removed")
  void onDeleteAddress_deleted_returnsDeleted() {
    var addressDao = mock(AddressDAO.class);
    when(addressDao.delete(tenantId, locationId, addressId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(addressDao));
    TestInbox<AddressActorResponse> inbox = TestInbox.create();

    testKit.run(new AddressCommand.DeleteAddress(tenantId, locationId, addressId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AddressActorResponse.Deleted.class);
  }
}
