package org.larpconnect.data.dao;

import org.larpconnect.data.entity.Individual;

/** DAO interface for managing Individual entities. */
public sealed interface IndividualDao extends BaseDao<Individual, IndividualDao>
    permits DefaultIndividualDao {}
