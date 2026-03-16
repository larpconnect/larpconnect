package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.entity.Entity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Provider;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultEntityDaoTest {

  @Mock private Provider<Mutiny.SessionFactory> sessionFactoryProvider;
  @Mock private Mutiny.SessionFactory sessionFactory;
  @Mock private Mutiny.Session session;

  private DefaultEntityDao entityDao;

  @BeforeEach
  void setUp() {
    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    entityDao = new DefaultEntityDao(sessionFactoryProvider);
  }

  @Test
  void findById_shouldReturnEntity_whenFound() {
    UUID id = UUID.randomUUID();
    Entity expectedEntity = org.mockito.Mockito.mock(Entity.class);

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<Entity>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.find(Entity.class, id)).thenReturn(Uni.createFrom().item(expectedEntity));

    Entity actualEntity = entityDao.findById(id).await().indefinitely();

    assertThat(actualEntity).isEqualTo(expectedEntity);
    verify(session).find(Entity.class, id);
  }

  @Test
  void persist_shouldReturnPersistedEntity() {
    Entity entityToPersist = org.mockito.Mockito.mock(Entity.class);

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<Entity>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.persist(entityToPersist)).thenReturn(Uni.createFrom().voidItem());
    when(session.flush()).thenReturn(Uni.createFrom().voidItem());

    Entity persistedEntity = entityDao.persist(entityToPersist).await().indefinitely();

    assertThat(persistedEntity).isEqualTo(entityToPersist);
    verify(session).persist(entityToPersist);
    verify(session).flush();
  }
