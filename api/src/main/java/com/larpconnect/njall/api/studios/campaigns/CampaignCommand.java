package com.larpconnect.njall.api.studios.campaigns;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link CampaignActor}. */
public sealed interface CampaignCommand {

  @Immutable
  record CreateCampaign(
      UUID tenantId,
      String studioIdentifier,
      CreateCampaignRequest request,
      ActorRef<CampaignActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements CampaignCommand {
    public CreateCampaign(
        UUID tenantId,
        String studioIdentifier,
        CreateCampaignRequest request,
        ActorRef<CampaignActorResponse> replyTo) {
      this(tenantId, studioIdentifier, request, replyTo, Optional.empty());
    }
  }

  @Immutable
  record GetCampaign(
      UUID tenantId,
      String studioIdentifier,
      UUID campaignId,
      ActorRef<CampaignActorResponse> replyTo)
      implements CampaignCommand {}

  @Immutable
  record ListCampaigns(
      UUID tenantId,
      String studioIdentifier,
      Optional<String> systemId,
      ActorRef<CampaignActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements CampaignCommand {
    public ListCampaigns(
        UUID tenantId,
        String studioIdentifier,
        Optional<String> systemId,
        ActorRef<CampaignActorResponse> replyTo) {
      this(tenantId, studioIdentifier, systemId, replyTo, Optional.empty());
    }
  }

  @Immutable
  record PatchCampaign(
      UUID tenantId,
      String studioIdentifier,
      UUID campaignId,
      UpdateCampaignRequest request,
      Optional<String> updateMask,
      ActorRef<CampaignActorResponse> replyTo,
      Optional<StudioLookup> lookup)
      implements CampaignCommand {
    public PatchCampaign(
        UUID tenantId,
        String studioIdentifier,
        UUID campaignId,
        UpdateCampaignRequest request,
        Optional<String> updateMask,
        ActorRef<CampaignActorResponse> replyTo) {
      this(tenantId, studioIdentifier, campaignId, request, updateMask, replyTo, Optional.empty());
    }
  }

  @Immutable
  record DeleteCampaign(UUID tenantId, UUID campaignId, ActorRef<CampaignActorResponse> replyTo)
      implements CampaignCommand {}
}
