package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Hashtag;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultHashtagDAO implements HashtagDAO {

  private static final String ENTITY_TYPE_HASHTAG = "Hashtag";
  private static final String LINK_TYPE_HASHTAG = "hashtag";
  private static final String DEFAULT_MEDIA_TYPE = "application/json";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultHashtagDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Hashtag> findById(UUID tenantId, UUID hashtagId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var result = loadHashtag(session, tenantId, hashtagId);
        tx.commit();
        return result;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Hashtag> findByTag(UUID tenantId, String tag) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var tagIdOpt = findTagIdByName(session, tenantId, tag);
        if (tagIdOpt.isEmpty()) {
          tx.commit();
          return Optional.empty();
        }
        var result = loadHashtag(session, tenantId, tagIdOpt.get());
        tx.commit();
        return result;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Hashtag> listAll(UUID tenantId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var rows =
            session
                .createQuery(
                    "SELECT e, l, h FROM EntityBaseEntity e, LinkEntity l, HashtagEntity h "
                        + "WHERE e.tenantId = :tenantId AND l.tenantId = :tenantId "
                        + "AND h.tenantId = :tenantId AND e.id = l.id AND l.id = h.id "
                        + "AND e.deletedOn IS NULL ORDER BY LOWER(h.tag) ASC",
                    Object[].class)
                .setParameter("tenantId", tenantId)
                .getResultList();
        tx.commit();
        var list = new ArrayList<Hashtag>(rows.size());
        for (var row : rows) {
          list.add(
              toHashtag((EntityBaseEntity) row[0], (LinkEntity) row[1], (HashtagEntity) row[2]));
        }
        return ImmutableList.copyOf(list);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Hashtag create(UUID tenantId, String tag, String canonicalUrl, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var hashtag = internalCreateOrResolve(session, tenantId, tag, canonicalUrl, summary);
        tx.commit();
        return hashtag;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Hashtag> batchCreate(UUID tenantId, List<TagCreationItem> items) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var list = new ArrayList<Hashtag>(items.size());
        for (var item : items) {
          list.add(
              internalCreateOrResolve(
                  session, tenantId, item.tag(), item.canonicalUrl(), item.summary()));
        }
        tx.commit();
        return ImmutableList.copyOf(list);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Hashtag> patch(
      UUID tenantId, UUID hashtagId, Optional<String> tag, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, hashtagId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var link = session.find(LinkEntity.class, entityId);
        var hashtagEntity = session.find(HashtagEntity.class, entityId);
        if (entity == null
            || link == null
            || hashtagEntity == null
            || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }
        tag.ifPresent(hashtagEntity::setTag);
        summary.ifPresent(entity::setSummary);
        entity.setUpdatedOn(Instant.now());
        session.merge(entity);
        session.merge(hashtagEntity);
        tx.commit();
        return Optional.of(toHashtag(entity, link, hashtagEntity));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID hashtagId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, hashtagId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        if (entity == null || entity.getDeletedOn() != null) {
          tx.commit();
          return false;
        }
        var now = Instant.now();
        entity.setDeletedOn(now);
        entity.setUpdatedOn(now);
        session.merge(entity);
        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Hashtag> list() {
    throw new UnsupportedOperationException(
        "Direct un-tenanted hashtag listing is not permitted in user space");
  }

  private Hashtag internalCreateOrResolve(
      Session session, UUID tenantId, String tag, String canonicalUrl, Optional<String> summary) {
    var existingIdOpt = findTagIdByName(session, tenantId, tag);
    if (existingIdOpt.isPresent()) {
      return reactivateOrReturn(session, tenantId, existingIdOpt.get(), tag, summary);
    }
    return persistNewHashtag(session, tenantId, tag, canonicalUrl, summary);
  }

  private Hashtag reactivateOrReturn(
      Session session, UUID tenantId, UUID hashtagId, String tag, Optional<String> summary) {
    var entityId = new EntityId(tenantId, hashtagId);
    var entity = session.find(EntityBaseEntity.class, entityId);
    var link = session.find(LinkEntity.class, entityId);
    var hashtagEntity = session.find(HashtagEntity.class, entityId);
    if (entity.getDeletedOn() != null) {
      entity.setDeletedOn(null);
      entity.setUpdatedOn(Instant.now());
      summary.ifPresent(entity::setSummary);
      hashtagEntity.setTag(tag);
      session.merge(entity);
      session.merge(hashtagEntity);
    }
    return toHashtag(entity, link, hashtagEntity);
  }

  private Hashtag persistNewHashtag(
      Session session, UUID tenantId, String tag, String canonicalUrl, Optional<String> summary) {
    var hashtagId = UUID.randomUUID();
    var now = Instant.now();
    var entity =
        new EntityBaseEntity(
            tenantId, hashtagId, ENTITY_TYPE_HASHTAG, summary.orElse(null), now, now, null);
    session.persist(entity);

    var link =
        new LinkEntity(tenantId, hashtagId, LINK_TYPE_HASHTAG, canonicalUrl, DEFAULT_MEDIA_TYPE);
    session.persist(link);

    var hashtagEntity = new HashtagEntity(tenantId, hashtagId, tag);
    session.persist(hashtagEntity);

    return toHashtag(entity, link, hashtagEntity);
  }

  /**
   * Resolves the primary identifier for a tag name under the specified tenant.
   *
   * <p>Uses {@code stream().findFirst()} on the result list rather than {@code
   * uniqueResultOptional()} to safely select the identifier without throwing {@code
   * NonUniqueResultException} in the presence of duplicate matches.
   */
  private Optional<UUID> findTagIdByName(Session session, UUID tenantId, String tag) {
    var lowerTag = tag.toLowerCase(Locale.ROOT);
    var query =
        session.createQuery(
            "SELECT h.id FROM HashtagEntity h WHERE h.tenantId = :tenantId "
                + "AND LOWER(h.tag) = :lowerTag",
            UUID.class);
    query.setParameter("tenantId", tenantId);
    query.setParameter("lowerTag", lowerTag);
    return query.getResultList().stream().findFirst();
  }

  private Optional<Hashtag> loadHashtag(Session session, UUID tenantId, UUID hashtagId) {
    var entityId = new EntityId(tenantId, hashtagId);
    var entity = session.find(EntityBaseEntity.class, entityId);
    var link = session.find(LinkEntity.class, entityId);
    var hashtag = session.find(HashtagEntity.class, entityId);
    if (entity == null || link == null || hashtag == null || entity.getDeletedOn() != null) {
      return Optional.empty();
    }
    return Optional.of(toHashtag(entity, link, hashtag));
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private static void rollbackQuietly(Transaction tx, Exception e) {
    try {
      tx.rollback();
    } catch (Exception rollbackException) {
      e.addSuppressed(rollbackException);
    }
  }

  private static Hashtag toHashtag(
      EntityBaseEntity entity, LinkEntity link, HashtagEntity hashtag) {
    return new Hashtag(
        entity.getId(),
        hashtag.getTag(),
        link.getLinkType(),
        link.getUrl(),
        link.getMediaType(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
