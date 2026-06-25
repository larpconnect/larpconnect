package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.Campaign;

/** Default implementation of CampaignDao. */
final class DefaultCampaignDao extends AbstractDao<Campaign, CampaignDao> implements CampaignDao {
  @Inject
  DefaultCampaignDao(SessionFactory sessionFactory) {
    super(sessionFactory, Campaign.class);
  }
}
