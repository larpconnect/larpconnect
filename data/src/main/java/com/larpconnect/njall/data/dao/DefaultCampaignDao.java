package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Campaign;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCampaignDao extends AbstractDao<Campaign, UUID> implements CampaignDao {

  @Inject
  DefaultCampaignDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Campaign.class);
  }
}
