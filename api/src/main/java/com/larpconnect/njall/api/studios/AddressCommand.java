package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link AddressActor}. */
public sealed interface AddressCommand {

  @Immutable
  record CreateAddress(
      UUID tenantId,
      UUID locationId,
      CreateAddressRequest request,
      ActorRef<AddressActorResponse> replyTo)
      implements AddressCommand {}

  @Immutable
  record GetAddress(
      UUID tenantId, UUID locationId, UUID addressId, ActorRef<AddressActorResponse> replyTo)
      implements AddressCommand {}

  @Immutable
  record ListAddresses(UUID tenantId, UUID locationId, ActorRef<AddressActorResponse> replyTo)
      implements AddressCommand {}

  @Immutable
  record PatchAddress(
      UUID tenantId,
      UUID locationId,
      UUID addressId,
      UpdateAddressRequest request,
      Optional<String> updateMask,
      ActorRef<AddressActorResponse> replyTo)
      implements AddressCommand {}

  @Immutable
  record DeleteAddress(
      UUID tenantId, UUID locationId, UUID addressId, ActorRef<AddressActorResponse> replyTo)
      implements AddressCommand {}
}
