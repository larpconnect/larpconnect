package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.Individual;

/** Default implementation of IndividualDao. */
final class DefaultIndividualDao extends AbstractDao<Individual, IndividualDao>
    implements IndividualDao {
  @Inject
  DefaultIndividualDao(SessionFactory sessionFactory) {
    super(sessionFactory, Individual.class);
  }
}
