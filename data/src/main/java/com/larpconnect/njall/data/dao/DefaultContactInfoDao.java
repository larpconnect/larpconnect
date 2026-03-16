package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.ContactInfo;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultContactInfoDao implements ContactInfoDao {
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultContactInfoDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Uni<ContactInfo> findById(UUID id) {
    return sessionFactoryProvider.get().withSession(session -> session.find(ContactInfo.class, id));
  }

  @Override
  public Uni<ContactInfo> persist(ContactInfo entity) {
    return sessionFactoryProvider
        .get()
        .withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }
}
