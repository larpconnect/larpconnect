package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Campaign;
import java.util.UUID;

@DefaultImplementation(DefaultCampaignDao.class)
public interface CampaignDao extends Dao<Campaign, UUID> {}
