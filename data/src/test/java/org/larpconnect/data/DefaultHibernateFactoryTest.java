package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultHibernateFactory}. */
public final class DefaultHibernateFactoryTest {
  @Test
  public void createRegistryBuilder_returnsNonNull() {
    DefaultHibernateFactory factory = new DefaultHibernateFactory();
    assertThat(factory.createRegistryBuilder()).isNotNull();
  }

  @Test
  public void createMetadataSources_returnsNonNull() {
    DefaultHibernateFactory factory = new DefaultHibernateFactory();
    StandardServiceRegistry registry = mock(StandardServiceRegistry.class);
    assertThat(factory.createMetadataSources(registry)).isNotNull();
  }
}
