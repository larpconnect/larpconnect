package com.larpconnect.njall.api.studios.campaigns;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.api.studios.common.StudioResourceUriSupport;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import com.larpconnect.njall.data.dao.studios.campaigns.PatchCampaignParams;
import com.larpconnect.njall.data.domain.studios.Campaign;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted campaign operations. */
final class CampaignActor {

  private final Logger logger = LoggerFactory.getLogger(CampaignActor.class);
  private final CampaignDAO campaignDao;

  private CampaignActor(CampaignDAO campaignDao) {
    this.campaignDao = campaignDao;
  }

  static Behavior<CampaignCommand> create(CampaignDAO campaignDao) {
    return createActor(campaignDao).behavior();
  }

  private static CampaignActor createActor(CampaignDAO campaignDao) {
    return new CampaignActor(campaignDao);
  }

  private Behavior<CampaignCommand> behavior() {
    return Behaviors.receiveMessage(this::onCommand);
  }

  private Behavior<CampaignCommand> onCommand(CampaignCommand command) {
    return switch (command) {
      case CampaignCommand.CreateCampaign cmd -> onCreateCampaign(cmd);
      case CampaignCommand.GetCampaign cmd -> onGetCampaign(cmd);
      case CampaignCommand.ListCampaigns cmd -> onListCampaigns(cmd);
      case CampaignCommand.PatchCampaign cmd -> onPatchCampaign(cmd);
      case CampaignCommand.DeleteCampaign cmd -> onDeleteCampaign(cmd);
    };
  }

  private Behavior<CampaignCommand> onCreateCampaign(CampaignCommand.CreateCampaign cmd) {
    try {
      var validationError = CampaignValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(CampaignActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      executeCreate(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create campaign", e);
    }
    return Behaviors.same();
  }

  private void executeCreate(CampaignCommand.CreateCampaign cmd) {
    var request = cmd.request();
    var generatorId =
        StudioResourceUriSupport.parseResourceUri(
            request.generator(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.ACTORS_COLLECTION,
            cmd.lookup());
    var systemId =
        StudioResourceUriSupport.parseOptionalResourceUri(
            request.system(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.SYSTEMS_COLLECTION,
            cmd.lookup());
    var domain =
        campaignDao.create(
            cmd.tenantId(), generatorId, request.title(), request.summary(), systemId);
    cmd.replyTo()
        .tell(CampaignActorResponse.success(toCampaignResponse(cmd.studioIdentifier(), domain)));
  }

  private Behavior<CampaignCommand> onGetCampaign(CampaignCommand.GetCampaign cmd) {
    try {
      executeGet(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get campaign", e);
    }
    return Behaviors.same();
  }

  private void executeGet(CampaignCommand.GetCampaign cmd) {
    campaignDao
        .findById(cmd.tenantId(), cmd.campaignId())
        .ifPresentOrElse(
            campaign ->
                cmd.replyTo()
                    .tell(
                        CampaignActorResponse.success(
                            toCampaignResponse(cmd.studioIdentifier(), campaign))),
            () ->
                cmd.replyTo()
                    .tell(
                        CampaignActorResponse.notFound("Campaign not found: " + cmd.campaignId())));
  }

  private Behavior<CampaignCommand> onListCampaigns(CampaignCommand.ListCampaigns cmd) {
    try {
      executeList(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list campaigns", e);
    }
    return Behaviors.same();
  }

  private void executeList(CampaignCommand.ListCampaigns cmd) {
    var systemId =
        StudioResourceUriSupport.parseOptionalResourceUri(
            cmd.systemId(),
            cmd.studioIdentifier(),
            StudioResourceUriSupport.SYSTEMS_COLLECTION,
            cmd.lookup());
    var campaigns =
        campaignDao.listAll(cmd.tenantId(), systemId).stream()
            .map(campaign -> toCampaignResponse(cmd.studioIdentifier(), campaign))
            .collect(toImmutableList());
    cmd.replyTo().tell(CampaignActorResponse.items(campaigns));
  }

  private Behavior<CampaignCommand> onPatchCampaign(CampaignCommand.PatchCampaign cmd) {
    try {
      var maskFields = UpdateMaskSupport.parseUpdateMask(cmd.updateMask());
      var validationError = CampaignValidation.validatePatch(cmd.request(), maskFields);
      if (validationError.isPresent()) {
        cmd.replyTo().tell(CampaignActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      executePatch(cmd, maskFields);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch campaign", e);
    }
    return Behaviors.same();
  }

  private void executePatch(
      CampaignCommand.PatchCampaign cmd, Optional<ImmutableSet<String>> maskFields) {
    var params = buildPatchParams(cmd, maskFields);
    campaignDao
        .patch(cmd.tenantId(), cmd.campaignId(), params)
        .ifPresentOrElse(
            updated ->
                cmd.replyTo()
                    .tell(
                        CampaignActorResponse.success(
                            toCampaignResponse(cmd.studioIdentifier(), updated))),
            () ->
                cmd.replyTo()
                    .tell(
                        CampaignActorResponse.notFound("Campaign not found: " + cmd.campaignId())));
  }

  private Behavior<CampaignCommand> onDeleteCampaign(CampaignCommand.DeleteCampaign cmd) {
    try {
      executeDelete(cmd);
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete campaign", e);
    }
    return Behaviors.same();
  }

  private void executeDelete(CampaignCommand.DeleteCampaign cmd) {
    var deleted = campaignDao.softDelete(cmd.tenantId(), cmd.campaignId());
    if (deleted) {
      cmd.replyTo().tell(CampaignActorResponse.deleted());
    } else {
      cmd.replyTo().tell(CampaignActorResponse.notFound("Campaign not found: " + cmd.campaignId()));
    }
  }

  private static PatchCampaignParams buildPatchParams(
      CampaignCommand.PatchCampaign cmd, Optional<ImmutableSet<String>> maskFields) {
    var request = cmd.request();
    var title =
        UpdateMaskSupport.selectOptionalField(
            request.title(), Optional.empty(), maskFields, "title");
    var clearSummary = UpdateMaskSupport.isFieldCleared(maskFields, request.summary(), "summary");
    var summary =
        UpdateMaskSupport.selectOptionalField(
            request.summary(), Optional.empty(), maskFields, "summary");
    var clearSystem = UpdateMaskSupport.isFieldCleared(maskFields, request.system(), "system");
    var rawSystem =
        UpdateMaskSupport.selectOptionalField(
            request.system(), Optional.empty(), maskFields, "system");
    var systemId =
        StudioResourceUriSupport.parseOptionalResourceUri(
            rawSystem,
            cmd.studioIdentifier(),
            StudioResourceUriSupport.SYSTEMS_COLLECTION,
            cmd.lookup());
    return createPatchParams(title, summary, clearSummary, systemId, clearSystem);
  }

  private static PatchCampaignParams createPatchParams(
      Optional<String> title,
      Optional<String> summary,
      boolean clearSummary,
      Optional<UUID> systemId,
      boolean clearSystem) {
    return new PatchCampaignParams(title, summary, clearSummary, systemId, clearSystem);
  }

  private static CampaignResponse toCampaignResponse(String studioIdentifier, Campaign campaign) {
    var generatorUri =
        StudioResourceUriSupport.formatResourceUri(
            studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, campaign.generator());
    var systemUri =
        campaign
            .systemId()
            .map(
                sysId ->
                    StudioResourceUriSupport.formatResourceUri(
                        studioIdentifier, StudioResourceUriSupport.SYSTEMS_COLLECTION, sysId));
    return createCampaignResponse(campaign, generatorUri, systemUri);
  }

  private static CampaignResponse createCampaignResponse(
      Campaign campaign, String generatorUri, Optional<String> systemUri) {
    return new CampaignResponse(
        campaign.id(),
        generatorUri,
        campaign.title(),
        campaign.summary(),
        systemUri,
        campaign.createdOn(),
        campaign.updatedOn());
  }

  private void handleError(
      ActorRef<CampaignActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in CampaignActor", operation, error);
    if (error instanceof IllegalArgumentException) {
      var message = error.getMessage();
      var reason = !Strings.isNullOrEmpty(message) ? message : "Invalid campaign request";
      replyTo.tell(CampaignActorResponse.badRequest(reason));
      return;
    }
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(CampaignActorResponse.failure(reason));
  }
}
