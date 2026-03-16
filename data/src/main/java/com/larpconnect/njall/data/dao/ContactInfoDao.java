package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ContactInfo;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultContactInfoDao.class)
public interface ContactInfoDao {
  Uni<ContactInfo> findById(UUID id);

  Uni<ContactInfo> persist(ContactInfo entity);
}
