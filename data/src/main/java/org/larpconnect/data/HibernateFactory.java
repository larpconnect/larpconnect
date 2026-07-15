package org.larpconnect.data;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/** Interface for isolating Hibernate boot class instantiations. */
interface HibernateFactory {
  StandardServiceRegistryBuilder createRegistryBuilder();

  MetadataSources createMetadataSources(StandardServiceRegistry registry);
}
