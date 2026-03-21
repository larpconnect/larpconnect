package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.ContactInfo;
import java.util.UUID;

/** DAO for ContactInfo. */
@DefaultImplementation(DefaultContactInfoDao.class)
public interface ContactInfoDao extends Dao<ContactInfo, UUID> {}
