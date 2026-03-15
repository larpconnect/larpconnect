package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.entity.Actor;
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
class DefaultActorDaoTest {

  @Mock private Provider<Mutiny.SessionFactory> sessionFactoryProvider;
  @Mock private Mutiny.SessionFactory sessionFactory;
  @Mock private Mutiny.Session session;

  private DefaultActorDao actorDao;

  @BeforeEach
  void setUp() {
    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    actorDao = new DefaultActorDao(sessionFactoryProvider);
  }

  @Test
  void findById_shouldReturnActor_whenFound() {
    UUID id = UUID.randomUUID();
    Actor expectedActor = new Actor();
    expectedActor.setId(id);

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<Actor>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.find(Actor.class, id)).thenReturn(Uni.createFrom().item(expectedActor));

    Actor actualActor = actorDao.findById(id).await().indefinitely();

    assertThat(actualActor).isEqualTo(expectedActor);
    verify(session).find(Actor.class, id);
  }

  @Test
  void persist_shouldReturnPersistedActor() {
    Actor actorToPersist = new Actor();

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<Actor>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.persist(actorToPersist)).thenReturn(Uni.createFrom().voidItem());
    when(session.flush()).thenReturn(Uni.createFrom().voidItem());

    Actor persistedActor = actorDao.persist(actorToPersist).await().indefinitely();

    assertThat(persistedActor).isEqualTo(actorToPersist);
    verify(session).persist(actorToPersist);
    verify(session).flush();
  }
}
