package com.larpconnect.njall.api.studios.campaigns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Response protocol emitted by {@link CampaignActor}. */
public sealed interface CampaignActorResponse {

  @Immutable
  record Success(CampaignResponse campaign) implements CampaignActorResponse {}

  @Immutable
  record Items(ImmutableList<CampaignResponse> campaigns) implements CampaignActorResponse {}

  @Immutable
  record Deleted() implements CampaignActorResponse {}

  @Immutable
  record Failure(int status, String message) implements CampaignActorResponse {}

  static CampaignActorResponse success(CampaignResponse campaign) {
    return new Success(campaign);
  }

  static CampaignActorResponse items(ImmutableList<CampaignResponse> campaigns) {
    return new Items(campaigns);
  }

  static CampaignActorResponse deleted() {
    return new Deleted();
  }

  static CampaignActorResponse notFound(String message) {
    return new Failure(404, message);
  }

  static CampaignActorResponse badRequest(String message) {
    return new Failure(400, message);
  }

  static CampaignActorResponse failure(String message) {
    return new Failure(500, message);
  }
}
