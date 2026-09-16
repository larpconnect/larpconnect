package com.larpconnect.njall.data.dao;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.Server;
import com.larpconnect.njall.data.domain.ServerContact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

final class DefaultServerDAO implements ServerDAO {

  private final SessionFactory sessionFactory;

  @Inject
  DefaultServerDAO(@NjallAdmin SessionFactory sessionFactory) {
    this.sessionFactory = requireNonNull(sessionFactory, "sessionFactory cannot be null");
  }

  @Override
  public Optional<Server> findById(UUID id) {
    requireNonNull(id, "id cannot be null");
    try (var session = sessionFactory.openSession()) {
      return executeFindById(session, id);
    }
  }

  @Override
  public ImmutableList<Server> list() {
    try (var session = sessionFactory.openSession()) {
      return executeList(session);
    }
  }

  private Optional<Server> executeFindById(Session session, UUID id) {
    var entity = findServerEntity(session, id);
    if (entity == null) {
      return Optional.empty();
    }
    var contacts = loadContacts(session);
    return Optional.of(toServer(entity, contacts));
  }

  private ImmutableList<Server> executeList(Session session) {
    var serverEntities = findAllServerEntities(session);
    if (serverEntities.isEmpty()) {
      return ImmutableList.of();
    }
    var contacts = loadContacts(session);
    return assembleServers(serverEntities, contacts);
  }

  private ImmutableList<ServerContact> loadContacts(Session session) {
    var contactEntities = findAllContactEntities(session);
    return assembleContacts(contactEntities);
  }

  private ServerEntity findServerEntity(Session session, UUID id) {
    return session.find(ServerEntity.class, id);
  }

  private List<ServerEntity> findAllServerEntities(Session session) {
    return session.createQuery("from ServerEntity", ServerEntity.class).list();
  }

  private List<ServerContactEntity> findAllContactEntities(Session session) {
    var hql = "from ServerContactEntity order by ordering asc";
    return session.createQuery(hql, ServerContactEntity.class).list();
  }

  private Server toServer(ServerEntity entity, ImmutableList<ServerContact> contacts) {
    return Server.of(
        entity.getId(),
        entity.getName(),
        entity.getPrimaryDomain(),
        entity.getCreatedOn(),
        contacts);
  }

  private ImmutableList<Server> assembleServers(
      List<ServerEntity> entities, ImmutableList<ServerContact> contacts) {
    return entities.stream()
        .map(
            entity ->
                Server.of(
                    entity.getId(),
                    entity.getName(),
                    entity.getPrimaryDomain(),
                    entity.getCreatedOn(),
                    contacts))
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<ServerContact> assembleContacts(List<ServerContactEntity> entities) {
    return entities.stream()
        .map(
            entity ->
                ServerContact.of(
                    entity.getId(),
                    entity.getRoleType(),
                    entity.getContactType(),
                    entity.getContact(),
                    entity.getOrdering()))
        .collect(ImmutableList.toImmutableList());
  }
}
