package org.larpconnect.data;

import com.google.inject.Inject;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/** Default implementation of {@link HibernateFactory}. */
final class DefaultHibernateFactory implements HibernateFactory {
  @Inject
  DefaultHibernateFactory() {}

  @Override
  public StandardServiceRegistryBuilder createRegistryBuilder() {
    return new StandardServiceRegistryBuilder();
  }

  @Override
  public MetadataSources createMetadataSources(StandardServiceRegistry registry) {
    return new MetadataSources(registry);
  }
}
