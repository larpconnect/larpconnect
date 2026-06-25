package org.larpconnect.data;

import com.google.inject.AbstractModule;
import org.larpconnect.data.dao.DaoModule;
import org.larpconnect.data.entity.EntityModule;
import org.larpconnect.data.hibernate.HibernateModule;
import org.larpconnect.data.schema.SchemaModule;

/** Exposes bindings for the database layer (Hibernate/PostgreSQL). */
public final class DataModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SchemaModule());
    install(new EntityModule());
    install(new DaoModule());
    install(new HibernateModule());
  }
}
