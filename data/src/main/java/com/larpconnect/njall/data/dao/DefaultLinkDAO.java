package com.larpconnect.njall.data.dao;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.domain.Link;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

final class DefaultLinkDAO implements LinkDAO {

  private static final String ENTITY_TYPE_LINK = "Link";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultLinkDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Link> findById(UUID tenantId, UUID linkId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, linkId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var link = session.find(LinkEntity.class, entityId);
        tx.commit();
        if (entity == null || link == null || entity.getDeletedOn() != null) {
          return Optional.empty();
        }
        return Optional.of(toLink(entity, link));
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public Link create(
      UUID tenantId, String linkType, String url, String mediaType, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var insertEntitySql =
            "INSERT INTO njall_users.entities (tenant_id, entity_type, summary) "
                + "VALUES (:tenantId, :entityType, :summary) "
                + "RETURNING id";
        var linkId =
            session
                .createNativeQuery(insertEntitySql, UUID.class)
                .setParameter("tenantId", tenantId)
                .setParameter("entityType", ENTITY_TYPE_LINK)
                .setParameter("summary", summary.orElse(null))
                .getSingleResult();

        var insertLinkSql =
            "INSERT INTO njall_users.links (tenant_id, id, link_type, url, media_type) "
                + "VALUES (:tenantId, :linkId, :linkType, :url, :mediaType)";
        session
            .createNativeQuery(insertLinkSql, Void.class)
            .setParameter("tenantId", tenantId)
            .setParameter("linkId", linkId)
            .setParameter("linkType", linkType)
            .setParameter("url", url)
            .setParameter("mediaType", mediaType)
            .executeUpdate();

        var entityId = new EntityId(tenantId, linkId);
        var entity =
            requireNonNull(
                session.find(EntityBaseEntity.class, entityId), "Persisted entity cannot be null");
        var link =
            requireNonNull(
                session.find(LinkEntity.class, entityId), "Persisted link cannot be null");
        tx.commit();
        return toLink(entity, link);
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public Optional<Link> patch(
      UUID tenantId,
      UUID linkId,
      Optional<String> linkType,
      Optional<String> url,
      Optional<String> mediaType,
      Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, linkId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var link = session.find(LinkEntity.class, entityId);
        if (entity == null || link == null || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }
        summary.ifPresent(entity::setSummary);
        entity.setUpdatedOn(Instant.now());
        linkType.ifPresent(link::setLinkType);
        url.ifPresent(link::setUrl);
        mediaType.ifPresent(link::setMediaType);
        session.merge(entity);
        session.merge(link);
        tx.commit();
        return Optional.of(toLink(entity, link));
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID linkId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, linkId);
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
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Link> list() {
    throw new UnsupportedOperationException(
        "Multi-tenant link listing is not permitted in user space");
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, true)", String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private static Link toLink(EntityBaseEntity entity, LinkEntity link) {
    return new Link(
        entity.getId(),
        link.getLinkType(),
        link.getUrl(),
        link.getMediaType(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
