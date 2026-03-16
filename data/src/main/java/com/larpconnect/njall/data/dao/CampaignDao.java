package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Campaign;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultCampaignDao.class)
public interface CampaignDao {
  Uni<Campaign> findById(UUID id);

  Uni<Campaign> persist(Campaign entity);
}
