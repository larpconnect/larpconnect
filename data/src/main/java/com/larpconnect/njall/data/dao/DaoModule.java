package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;

public final class DaoModule extends AbstractModule {
  public DaoModule() {}

  @Override
  protected void configure() {
    bind(EntityDao.class).to(DefaultEntityDao.class);
    bind(ActorDao.class).to(DefaultActorDao.class);
    bind(UserDao.class).to(DefaultUserDao.class);
    bind(ActorEndpointDao.class).to(DefaultActorEndpointDao.class);
    bind(CollectionDao.class).to(DefaultCollectionDao.class);
    bind(CollectionItemDao.class).to(DefaultCollectionItemDao.class);
    bind(ContactInfoDao.class).to(DefaultContactInfoDao.class);
    bind(IndividualDao.class).to(DefaultIndividualDao.class);
    bind(StudioDao.class).to(DefaultStudioDao.class);
    bind(RoleDao.class).to(DefaultRoleDao.class);
    bind(StudioRoleDao.class).to(DefaultStudioRoleDao.class);
    bind(LocationDao.class).to(DefaultLocationDao.class);
    bind(LarpSystemDao.class).to(DefaultLarpSystemDao.class);
    bind(CampaignDao.class).to(DefaultCampaignDao.class);
    bind(GameDao.class).to(DefaultGameDao.class);
    bind(LarpCharacterDao.class).to(DefaultLarpCharacterDao.class);
    bind(CharacterInstanceDao.class).to(DefaultCharacterInstanceDao.class);
    bind(ServerMetadataDao.class).to(DefaultServerMetadataDao.class);
  }
}
