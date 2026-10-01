package com.larpconnect.njall.data.dao;

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
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

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
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
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
        var linkId = UUID.randomUUID();
        var now = Instant.now();
        var entity =
            new EntityBaseEntity(
                tenantId, linkId, ENTITY_TYPE_LINK, summary.orElse(null), now, now, null);
        session.persist(entity);

        var link = new LinkEntity(tenantId, linkId, linkType, url, mediaType);
        session.persist(link);

        tx.commit();
        return toLink(entity, link);
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
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
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
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
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
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
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
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
