package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.ContactInfo;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultContactInfoDao extends AbstractDao<ContactInfo, UUID> implements ContactInfoDao {

  @Inject
  DefaultContactInfoDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, ContactInfo.class);
  }
}
