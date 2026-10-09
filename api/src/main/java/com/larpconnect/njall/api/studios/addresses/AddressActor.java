package com.larpconnect.njall.api.studios.addresses;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.domain.Address;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space subordinate location address operations. */
final class AddressActor extends AbstractBehavior<AddressCommand> {

  private final Logger logger = LoggerFactory.getLogger(AddressActor.class);
  private final AddressDAO addressDao;

  AddressActor(ActorContext<AddressCommand> context, AddressDAO addressDao) {
    super(context);
    this.addressDao = addressDao;
  }

  @Override
  public Receive<AddressCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(AddressCommand.CreateAddress.class, this::onCreateAddress)
        .onMessage(AddressCommand.GetAddress.class, this::onGetAddress)
        .onMessage(AddressCommand.ListAddresses.class, this::onListAddresses)
        .onMessage(AddressCommand.PatchAddress.class, this::onPatchAddress)
        .onMessage(AddressCommand.DeleteAddress.class, this::onDeleteAddress)
        .build();
  }

  private Behavior<AddressCommand> onCreateAddress(AddressCommand.CreateAddress cmd) {
    try {
      var validationError = validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(AddressActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executeCreate(cmd.tenantId(), cmd.locationId(), cmd.request(), cmd.replyTo());
    } catch (IllegalArgumentException e) {
      cmd.replyTo().tell(AddressActorResponse.notFound(e.getMessage()));
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create address", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId,
      UUID locationId,
      CreateAddressRequest request,
      ActorRef<AddressActorResponse> replyTo) {
    var domain =
        addressDao
            .create(tenantId, locationId)
            .addressType(request.addressType())
            .addressLine1(request.addressLine1())
            .addressLine2(request.addressLine2())
            .addressLine3(request.addressLine3())
            .locality(request.locality())
            .administrativeArea(request.administrativeArea())
            .postalCode(request.postalCode())
            .countryCode(request.countryCode())
            .geom(request.geom())
            .execute();
    replyTo.tell(AddressActorResponse.success(toAddressResponse(domain)));
  }

  private Behavior<AddressCommand> onGetAddress(AddressCommand.GetAddress cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.locationId(), cmd.addressId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get address", e);
    }
    return this;
  }

  private void executeGet(
      UUID tenantId, UUID locationId, UUID addressId, ActorRef<AddressActorResponse> replyTo) {
    addressDao
        .findById(tenantId, locationId, addressId)
        .ifPresentOrElse(
            addr -> replyTo.tell(AddressActorResponse.success(toAddressResponse(addr))),
            () -> replyTo.tell(AddressActorResponse.notFound("Address not found: " + addressId)));
  }

  private Behavior<AddressCommand> onListAddresses(AddressCommand.ListAddresses cmd) {
    try {
      executeList(cmd.tenantId(), cmd.locationId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list addresses", e);
    }
    return this;
  }

  private void executeList(UUID tenantId, UUID locationId, ActorRef<AddressActorResponse> replyTo) {
    var list = addressDao.listByLocation(tenantId, locationId);
    var responses =
        list.stream().map(this::toAddressResponse).collect(ImmutableList.toImmutableList());
    replyTo.tell(AddressActorResponse.listSuccess(responses));
  }

  private Behavior<AddressCommand> onPatchAddress(AddressCommand.PatchAddress cmd) {
    try {
      var validationError = validatePatch(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(AddressActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executePatch(
          cmd.tenantId(),
          cmd.locationId(),
          cmd.addressId(),
          cmd.request(),
          cmd.updateMask(),
          cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch address", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      UUID locationId,
      UUID addressId,
      UpdateAddressRequest request,
      Optional<String> updateMask,
      ActorRef<AddressActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var patcher = addressDao.patch(tenantId, locationId, addressId);

    selectField(maskFields, "addressType", request.addressType()).ifPresent(patcher::addressType);
    selectField(maskFields, "addressLine1", request.addressLine1())
        .ifPresent(patcher::addressLine1);
    selectField(maskFields, "addressLine2", request.addressLine2())
        .ifPresent(patcher::addressLine2);
    selectField(maskFields, "addressLine3", request.addressLine3())
        .ifPresent(patcher::addressLine3);
    selectField(maskFields, "locality", request.locality()).ifPresent(patcher::locality);
    selectField(maskFields, "administrativeArea", request.administrativeArea())
        .ifPresent(patcher::administrativeArea);
    selectField(maskFields, "postalCode", request.postalCode()).ifPresent(patcher::postalCode);
    selectField(maskFields, "countryCode", request.countryCode()).ifPresent(patcher::countryCode);

    var updateGeom = maskFields.map(m -> m.contains("geom")).orElse(request.geom().isPresent());
    if (updateGeom) {
      patcher.geom(selectField(maskFields, "geom", request.geom()));
    }

    patcher
        .execute()
        .ifPresentOrElse(
            updated -> replyTo.tell(AddressActorResponse.success(toAddressResponse(updated))),
            () -> replyTo.tell(AddressActorResponse.notFound("Address not found: " + addressId)));
  }

  private AddressResponse toAddressResponse(Address a) {
    return new AddressResponse(
        a.id(),
        a.locationId(),
        a.addressType(),
        a.addressLine1(),
        a.addressLine2(),
        a.addressLine3(),
        a.locality(),
        a.administrativeArea(),
        a.postalCode(),
        a.countryCode(),
        a.geom());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private <T> Optional<T> selectField(
      Optional<Set<String>> maskFields, String fieldName, Optional<T> requestValue) {
    if (maskFields.isPresent()) {
      return maskFields.orElseThrow().contains(fieldName) ? requestValue : Optional.empty();
    }
    return requestValue;
  }

  private Behavior<AddressCommand> onDeleteAddress(AddressCommand.DeleteAddress cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.locationId(), cmd.addressId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete address", e);
    }
    return this;
  }

  private void executeDelete(
      UUID tenantId, UUID locationId, UUID addressId, ActorRef<AddressActorResponse> replyTo) {
    var deleted = addressDao.delete(tenantId, locationId, addressId);
    if (deleted) {
      replyTo.tell(AddressActorResponse.deleted());
    } else {
      replyTo.tell(AddressActorResponse.notFound("Address not found: " + addressId));
    }
  }

  private void handleError(
      ActorRef<AddressActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in AddressActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(AddressActorResponse.failure(reason));
  }

  static Optional<String> validateCreate(CreateAddressRequest request) {
    var lineErr = validateAddressLine1(request.addressLine1());
    if (lineErr.isPresent()) {
      return lineErr;
    }
    var areaErr = validateArea(request.locality(), request.administrativeArea());
    if (areaErr.isPresent()) {
      return areaErr;
    }
    return validatePostalAndCountry(request.postalCode(), request.countryCode());
  }

  static Optional<String> validatePatch(UpdateAddressRequest request) {
    if (request.addressLine1().isPresent()) {
      var err = validateAddressLine1(request.addressLine1().orElseThrow());
      if (err.isPresent()) {
        return err;
      }
    }
    if (request.postalCode().isPresent()) {
      var code = request.postalCode().orElseThrow();
      if (code.isBlank() || code.length() > 20) {
        return Optional.of("postalCode must not exceed 20 characters");
      }
    }
    if (request.countryCode().isPresent()
        && request.countryCode().orElseThrow().trim().length() != 2) {
      return Optional.of("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");
    }
    return Optional.empty();
  }

  private static Optional<String> validateAddressLine1(String addressLine1) {
    if (addressLine1.isBlank()) {
      return Optional.of("addressLine1 must not be blank");
    }
    if (addressLine1.length() > 255) {
      return Optional.of("addressLine1 must not exceed 255 characters");
    }
    return Optional.empty();
  }

  private static Optional<String> validateArea(String locality, String administrativeArea) {
    if (locality.isBlank()) {
      return Optional.of("locality must not be blank");
    }
    if (administrativeArea.isBlank()) {
      return Optional.of("administrativeArea must not be blank");
    }
    return Optional.empty();
  }

  private static Optional<String> validatePostalAndCountry(String postalCode, String countryCode) {
    if (postalCode.isBlank()) {
      return Optional.of("postalCode must not be blank");
    }
    if (postalCode.length() > 20) {
      return Optional.of("postalCode must not exceed 20 characters");
    }
    if (countryCode.trim().length() != 2) {
      return Optional.of("countryCode must be a 2-letter ISO 3166-1 alpha-2 code");
    }
    return Optional.empty();
  }
}
