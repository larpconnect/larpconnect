package com.larpconnect.njall.api.studios.campaigns;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import org.apache.pekko.actor.typed.Behavior;

final class DefaultCampaignActorFactory implements CampaignActorFactory {

  private final CampaignDAO campaignDao;

  @Inject
  DefaultCampaignActorFactory(CampaignDAO campaignDao) {
    this.campaignDao = campaignDao;
  }

  @Override
  public Behavior<CampaignCommand> create() {
    return CampaignActor.create(campaignDao);
  }
}
