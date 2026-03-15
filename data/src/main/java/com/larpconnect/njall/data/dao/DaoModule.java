package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;

public final class DaoModule extends AbstractModule {
  public DaoModule() {}

  @Override
  protected void configure() {
    bind(EntityDao.class).to(DefaultEntityDao.class);
    bind(ActorDao.class).to(DefaultActorDao.class);
    bind(UserDao.class).to(DefaultUserDao.class);
  }
}
