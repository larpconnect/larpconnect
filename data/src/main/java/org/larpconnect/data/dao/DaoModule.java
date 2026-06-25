package org.larpconnect.data.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/** Guice module for data access object bindings. */
public final class DaoModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(StudioDao.class).to(DefaultStudioDao.class).in(Singleton.class);
    bind(CampaignDao.class).to(DefaultCampaignDao.class).in(Singleton.class);
    bind(LarpSystemDao.class).to(DefaultLarpSystemDao.class).in(Singleton.class);
    bind(GameDao.class).to(DefaultGameDao.class).in(Singleton.class);
    bind(LarpCharacterDao.class).to(DefaultLarpCharacterDao.class).in(Singleton.class);
    bind(CharacterInstanceDao.class).to(DefaultCharacterInstanceDao.class).in(Singleton.class);
    bind(IndividualDao.class).to(DefaultIndividualDao.class).in(Singleton.class);
    bind(UserDao.class).to(DefaultUserDao.class).in(Singleton.class);
    bind(ActorDao.class).to(DefaultActorDao.class).in(Singleton.class);
    bind(CollectionDao.class).to(DefaultCollectionDao.class).in(Singleton.class);
  }
}
