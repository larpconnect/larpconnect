package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.domain.Studio;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultStudioDAO implements StudioDAO {

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultStudioDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Studio> findById(UUID tenantId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        session
            .createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, true)", String.class)
            .setParameter("tenantId", tenantId.toString())
            .getSingleResult();

        var hql = "from StudioEntity where id = :tenantId";
        var entity =
            session
                .createQuery(hql, StudioEntity.class)
                .setParameter("tenantId", tenantId)
                .uniqueResult();
        tx.commit();
        return Optional.ofNullable(entity).map(this::toStudio);
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Studio> list() {
    throw new UnsupportedOperationException(
        "Multi-tenant studio listing is not permitted in user space");
  }

  private Studio toStudio(StudioEntity entity) {
    return new Studio(entity.getId(), entity.getName());
  }
}
