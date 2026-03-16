package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Individual;
import java.util.UUID;

@DefaultImplementation(DefaultIndividualDao.class)
public interface IndividualDao extends Dao<Individual, UUID> {}
