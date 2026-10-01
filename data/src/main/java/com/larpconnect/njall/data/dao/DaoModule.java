package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;

/** Guice module configuring DAO bindings and registering JPA entity classes. */
public final class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerDAO.class).to(DefaultServerDAO.class).in(Scopes.SINGLETON);
    bind(AdminRoleDAO.class).to(DefaultAdminRoleDAO.class).in(Scopes.SINGLETON);
    bind(AdminUserDAO.class).to(DefaultAdminUserDAO.class).in(Scopes.SINGLETON);
    bind(StudioLookupDAO.class).to(DefaultStudioLookupDAO.class).in(Scopes.SINGLETON);
    bind(StudioDAO.class).to(DefaultStudioDAO.class).in(Scopes.SINGLETON);
    bind(StudioRoleDAO.class).to(DefaultStudioRoleDAO.class).in(Scopes.SINGLETON);
    bind(LinkDAO.class).to(DefaultLinkDAO.class).in(Scopes.SINGLETON);
    bind(LocationDAO.class).to(DefaultLocationDAO.class).in(Scopes.SINGLETON);
    bind(AddressDAO.class).to(DefaultAddressDAO.class).in(Scopes.SINGLETON);

    var adminEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    adminEntities.addBinding().toInstance(ServerEntity.class);
    adminEntities.addBinding().toInstance(ServerContactEntity.class);
    adminEntities.addBinding().toInstance(AdminRoleEntity.class);
    adminEntities.addBinding().toInstance(AdminUserEntity.class);
    adminEntities.addBinding().toInstance(StudioLookupEntity.class);
    adminEntities.addBinding().toInstance(StudioEntity.class);
    adminEntities.addBinding().toInstance(DefaultStudioRoleEntity.class);
    adminEntities.addBinding().toInstance(EntityBaseEntity.class);
    adminEntities.addBinding().toInstance(LinkEntity.class);
    adminEntities.addBinding().toInstance(LocationEntity.class);
    adminEntities.addBinding().toInstance(AddressEntity.class);

    var userEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallUsers.class);
    userEntities.addBinding().toInstance(StudioEntity.class);
    userEntities.addBinding().toInstance(DefaultStudioRoleEntity.class);
    userEntities.addBinding().toInstance(EntityBaseEntity.class);
    userEntities.addBinding().toInstance(LinkEntity.class);
    userEntities.addBinding().toInstance(LocationEntity.class);
    userEntities.addBinding().toInstance(AddressEntity.class);
  }
}
