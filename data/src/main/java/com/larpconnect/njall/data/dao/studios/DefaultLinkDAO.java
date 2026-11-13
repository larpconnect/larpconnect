package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Link;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

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
        var entityId = createEntityId(tenantId, linkId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var link = session.find(LinkEntity.class, entityId);
        tx.commit();
        if (entity == null || link == null || entity.getDeletedOn() != null) {
          return Optional.empty();
        }
        return Optional.of(toLink(entity, link));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Link create(
      UUID tenantId,
      UUID generator,
      String linkType,
      String url,
      String mediaType,
      Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        ActorGeneratorValidator.validateActorGenerator(session, tenantId, generator);
        var linkId = UUID.randomUUID();
        var now = Instant.now();
        var entity = createBaseEntity(tenantId, linkId, generator, summary, now);
        session.persist(entity);

        var link = createLinkEntity(tenantId, linkId, linkType, url, mediaType);
        session.persist(link);

        tx.commit();
        return toLink(entity, link);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
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
        var entityId = createEntityId(tenantId, linkId);
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
        rollbackQuietly(tx, e);
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
        var entityId = createEntityId(tenantId, linkId);
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

  private static void rollbackQuietly(Transaction tx, Exception e) {
    try {
      tx.rollback();
    } catch (Exception rollbackException) {
      e.addSuppressed(rollbackException);
    }
  }

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID id, UUID generator, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, id, generator, ENTITY_TYPE_LINK, summary.orElse(null), now, now, null);
  }

  private static LinkEntity createLinkEntity(
      UUID tenantId, UUID id, String linkType, String url, String mediaType) {
    return new LinkEntity(tenantId, id, linkType, url, mediaType);
  }

  private static Link toLink(EntityBaseEntity entity, LinkEntity link) {
    return new Link(
        entity.getId(),
        entity.getGenerator(),
        link.getLinkType(),
        link.getUrl(),
        link.getMediaType(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
