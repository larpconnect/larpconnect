package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.domain.Studio;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultStudioDAO implements StudioDAO {

  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

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
            .createNativeQuery(SET_TENANT_CONFIG, String.class)
            .setParameter("tenantId", tenantId.toString())
            .getSingleResult();

        var entity = session.find(StudioEntity.class, tenantId);
        tx.commit();
        return Optional.ofNullable(entity).map(this::toStudio);
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
  public ImmutableList<Studio> list() {
    throw new UnsupportedOperationException(
        "Multi-tenant studio listing is not permitted in user space");
  }

  private Studio toStudio(StudioEntity entity) {
    return new Studio(entity.getId(), entity.getName());
  }
}
