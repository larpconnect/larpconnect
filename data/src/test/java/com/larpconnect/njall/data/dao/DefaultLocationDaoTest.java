package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.entity.Location;
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
class DefaultLocationDaoTest {

  @Mock private Provider<Mutiny.SessionFactory> sessionFactoryProvider;
  @Mock private Mutiny.SessionFactory sessionFactory;
  @Mock private Mutiny.Session session;

  private DefaultLocationDao dao;

  @BeforeEach
  void setUp() {
    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    dao = new DefaultLocationDao(sessionFactoryProvider);
  }

  @Test
  void findById_shouldReturnEntity_whenFound() {
    UUID id = null;
    Location expectedEntity = org.mockito.Mockito.mock(Location.class);

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<Location>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.find(Location.class, id)).thenReturn(Uni.createFrom().item(expectedEntity));

    Location actualEntity = dao.findById(id).await().indefinitely();

    assertThat(actualEntity).isEqualTo(expectedEntity);
    verify(session).find(Location.class, id);
  }

  @Test
  void persist_shouldReturnPersistedEntity() {
    Location entityToPersist = org.mockito.Mockito.mock(Location.class);

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<Location>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.persist(entityToPersist)).thenReturn(Uni.createFrom().voidItem());
    when(session.flush()).thenReturn(Uni.createFrom().voidItem());

    Location persistedEntity = dao.persist(entityToPersist).await().indefinitely();

    assertThat(persistedEntity).isEqualTo(entityToPersist);
    verify(session).persist(entityToPersist);
    verify(session).flush();
  }
