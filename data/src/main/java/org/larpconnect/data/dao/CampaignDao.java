package org.larpconnect.data.dao;

import org.larpconnect.data.entity.Campaign;

/** DAO interface for managing Campaign entities. */
public sealed interface CampaignDao extends BaseDao<Campaign, CampaignDao>
    permits DefaultCampaignDao {}
