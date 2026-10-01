package com.larpconnect.njall.api.studios;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link AddressActor}. */
public sealed interface AddressActorResponse {

  @Immutable
  record Success(AddressResponse address) implements AddressActorResponse {}

  @Immutable
  record ListSuccess(ImmutableList<AddressResponse> addresses) implements AddressActorResponse {}

  @Immutable
  record Deleted() implements AddressActorResponse {}

  @Immutable
  record NotFound(String message) implements AddressActorResponse {}

  @Immutable
  record BadRequest(String message) implements AddressActorResponse {}

  @Immutable
  record Failure(String message) implements AddressActorResponse {}

  static AddressActorResponse success(AddressResponse address) {
    return new Success(address);
  }

  static AddressActorResponse listSuccess(ImmutableList<AddressResponse> addresses) {
    return new ListSuccess(addresses);
  }

  static AddressActorResponse deleted() {
    return new Deleted();
  }

  static AddressActorResponse notFound(String message) {
    return new NotFound(message);
  }

  static AddressActorResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static AddressActorResponse failure(String message) {
    return new Failure(message);
  }
}
