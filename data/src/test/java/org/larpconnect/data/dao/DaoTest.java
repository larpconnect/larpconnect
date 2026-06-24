package org.larpconnect.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.entity.Campaign;
import org.larpconnect.data.entity.Studio;

public final class DaoTest {
  private SessionFactory sessionFactory;
  private Session session;

  @BeforeEach
  public void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.getCurrentSession()).thenReturn(session);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testStudioDao() {
    StudioDao dao = new DefaultStudioDao(sessionFactory);
    UUID id = UUID.randomUUID();
    Studio studio = new Studio(id, "Studio Name", "schema");

    // findById - not found
    when(session.find(Studio.class, id)).thenReturn(null);
    assertThat(dao.findById("schema", id)).isEmpty();

    // findById - null id
    assertThat(dao.findById("schema", null)).isEmpty();

    // findById - active studio
    when(session.find(Studio.class, id)).thenReturn(studio);
    assertThat(dao.findById("schema", id)).hasValue(studio);

    // findById - deleted studio
    studio.setDeletedAt(Instant.now());
    assertThat(dao.findById("schema", id)).isEmpty();
    studio.setDeletedAt(null); // restore

    // save
    dao.save("schema", studio);
    verify(session, times(1)).persist(studio);

    // delete (soft delete for Studio)
    dao.delete("schema", studio);
    assertThat(studio.getDeletedAt()).isNotNull();
    verify(session, times(1)).merge(studio);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCampaignDao() {
    CampaignDao dao = new DefaultCampaignDao(sessionFactory);
    UUID id = UUID.randomUUID();
    Campaign campaign = new Campaign(id, null);

    // findById - active
    when(session.find(Campaign.class, id)).thenReturn(campaign);
    assertThat(dao.findById("schema", id)).hasValue(campaign);

    // findById - soft deleted
    campaign.setDeletedOn(Instant.now());
    assertThat(dao.findById("schema", id)).isEmpty();
    campaign.setDeletedOn(null);

    // delete (soft delete for TenantEntity)
    dao.delete("schema", campaign);
    assertThat(campaign.getDeletedOn()).isNotNull();
    verify(session, times(1)).merge(campaign);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFindAll() {
    StudioDao studioDao = new DefaultStudioDao(sessionFactory);
    org.hibernate.query.criteria.HibernateCriteriaBuilder cb =
        mock(org.hibernate.query.criteria.HibernateCriteriaBuilder.class);
    org.hibernate.query.criteria.JpaCriteriaQuery<Studio> query =
        mock(org.hibernate.query.criteria.JpaCriteriaQuery.class);
    org.hibernate.query.criteria.JpaRoot<Studio> root =
        mock(org.hibernate.query.criteria.JpaRoot.class);
    Query<Studio> hibernateQuery = mock(Query.class);

    when(session.getCriteriaBuilder()).thenReturn(cb);
    when(cb.createQuery(Studio.class)).thenReturn(query);
    when(query.from(Studio.class)).thenReturn(root);
    when(session.createQuery(query)).thenReturn(hibernateQuery);

    List<Studio> studios = List.of(new Studio());
    when(hibernateQuery.getResultList()).thenReturn(studios);

    List<Studio> results = studioDao.findAll("schema");
    assertThat(results).isEqualTo(studios);

    // Test CampaignDao findAll (TenantEntity branch)
    CampaignDao campaignDao = new DefaultCampaignDao(sessionFactory);
    org.hibernate.query.criteria.JpaCriteriaQuery<Campaign> campaignQuery =
        mock(org.hibernate.query.criteria.JpaCriteriaQuery.class);
    org.hibernate.query.criteria.JpaRoot<Campaign> campaignRoot =
        mock(org.hibernate.query.criteria.JpaRoot.class);
    Query<Campaign> campaignHibernateQuery = mock(Query.class);

    when(cb.createQuery(Campaign.class)).thenReturn(campaignQuery);
    when(campaignQuery.from(Campaign.class)).thenReturn(campaignRoot);
    when(session.createQuery(campaignQuery)).thenReturn(campaignHibernateQuery);

    List<Campaign> campaigns = List.of(new Campaign());
    when(campaignHibernateQuery.getResultList()).thenReturn(campaigns);

    List<Campaign> campaignResults = campaignDao.findAll("schema");
    assertThat(campaignResults).isEqualTo(campaigns);
  }

  @Test
  public void testAllOtherConcreteDaosInstantiation() {
    // Basic coverage for instantiation of other concrete DAOs
    assertThat(new DefaultLarpSystemDao(sessionFactory)).isNotNull();
    assertThat(new DefaultGameDao(sessionFactory)).isNotNull();
    assertThat(new DefaultLarpCharacterDao(sessionFactory)).isNotNull();
    assertThat(new DefaultCharacterInstanceDao(sessionFactory)).isNotNull();
    assertThat(new DefaultIndividualDao(sessionFactory)).isNotNull();
    assertThat(new DefaultUserDao(sessionFactory)).isNotNull();
    assertThat(new DefaultActorDao(sessionFactory)).isNotNull();
    assertThat(new DefaultCollectionDao(sessionFactory)).isNotNull();
  }
}
