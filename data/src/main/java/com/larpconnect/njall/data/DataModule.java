package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.data.dao.ActorDao;
import com.larpconnect.njall.data.dao.DefaultActorDao;
import com.larpconnect.njall.data.dao.DefaultEntityDao;
import com.larpconnect.njall.data.dao.DefaultUserDao;
import com.larpconnect.njall.data.dao.EntityDao;
import com.larpconnect.njall.data.dao.UserDao;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

public final class DataModule extends AbstractModule {
  public DataModule() {}

  @Override
  protected void configure() {
    bind(EntityDao.class).to(DefaultEntityDao.class);
    bind(ActorDao.class).to(DefaultActorDao.class);
    bind(UserDao.class).to(DefaultUserDao.class);
  }

  @Provides
  @Singleton
  public Mutiny.SessionFactory provideSessionFactory() {
    return Persistence.createEntityManagerFactory("larpconnect")
        .unwrap(Mutiny.SessionFactory.class);
  }
}
