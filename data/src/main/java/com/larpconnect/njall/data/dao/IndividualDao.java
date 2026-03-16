package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Individual;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultIndividualDao.class)
public interface IndividualDao {
  Uni<Individual> findById(UUID id);

  Uni<Individual> persist(Individual entity);
}
