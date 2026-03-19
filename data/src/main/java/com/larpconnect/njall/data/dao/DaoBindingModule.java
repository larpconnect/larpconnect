package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(DaoModule.class)
final class DaoBindingModule extends AbstractModule {
  DaoBindingModule() {}

  @Override
  protected void configure() {
    bind(ExternalResourceDao.class).to(DefaultExternalResourceDao.class);
    bind(ContactInfoDao.class).to(DefaultContactInfoDao.class);
    bind(ServerMetadataDao.class).to(DefaultServerMetadataDao.class);
    bind(ActorDao.class).to(DefaultActorDao.class);
    bind(ActorEndpointDao.class).to(DefaultActorEndpointDao.class);
    bind(CollectionDao.class).to(DefaultCollectionDao.class);
    bind(CollectionItemDao.class).to(DefaultCollectionItemDao.class);
    bind(IndividualDao.class).to(DefaultIndividualDao.class);
    bind(UserDao.class).to(DefaultUserDao.class);
    bind(StudioDao.class).to(DefaultStudioDao.class);
    bind(RoleDao.class).to(DefaultRoleDao.class);
    bind(StudioRoleDao.class).to(DefaultStudioRoleDao.class);
    bind(LocationDao.class).to(DefaultLocationDao.class);
    bind(SystemDao.class).to(DefaultSystemDao.class);
    bind(CampaignDao.class).to(DefaultCampaignDao.class);
    bind(GameDao.class).to(DefaultGameDao.class);
    bind(CharacterDao.class).to(DefaultCharacterDao.class);
    bind(CharacterInstanceDao.class).to(DefaultCharacterInstanceDao.class);
  }
}
