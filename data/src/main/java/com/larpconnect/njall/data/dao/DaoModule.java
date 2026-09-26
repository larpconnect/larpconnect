package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;

/** Guice module configuring DAO bindings and registering JPA entity classes. */
public final class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerDAO.class).to(DefaultServerDAO.class);
    bind(AdminRoleDAO.class).to(DefaultAdminRoleDAO.class);
    bind(AdminUserDAO.class).to(DefaultAdminUserDAO.class);
    bind(StudioLookupDAO.class).to(DefaultStudioLookupDAO.class);
    bind(StudioDAO.class).to(DefaultStudioDAO.class);
    bind(DefaultStudioRoleDAO.class).to(DefaultDefaultStudioRoleDAO.class);

    var adminEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    adminEntities.addBinding().toInstance(ServerEntity.class);
    adminEntities.addBinding().toInstance(ServerContactEntity.class);
    adminEntities.addBinding().toInstance(AdminRoleEntity.class);
    adminEntities.addBinding().toInstance(AdminUserEntity.class);
    adminEntities.addBinding().toInstance(StudioLookupEntity.class);
    adminEntities.addBinding().toInstance(StudioEntity.class);
    adminEntities.addBinding().toInstance(DefaultStudioRoleEntity.class);

    var userEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallUsers.class);
    userEntities.addBinding().toInstance(StudioEntity.class);
    userEntities.addBinding().toInstance(DefaultStudioRoleEntity.class);
  }
}
