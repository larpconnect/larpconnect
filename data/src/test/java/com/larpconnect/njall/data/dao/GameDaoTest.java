package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.entity.Game;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Provider;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
final class GameDaoTest {

  private Mutiny.SessionFactory sessionFactoryMock;
  private Mutiny.Session sessionMock;
  private Provider<Mutiny.SessionFactory> providerMock;
  private DefaultGameDao dao;

  @BeforeEach
  void setUp() {
    sessionFactoryMock = mock(Mutiny.SessionFactory.class);
    sessionMock = mock(Mutiny.Session.class);
    providerMock = (Provider<Mutiny.SessionFactory>) mock(Provider.class);

    when(providerMock.get()).thenReturn(sessionFactoryMock);

    when(sessionFactoryMock.withSession((Function<Mutiny.Session, Uni<Object>>) any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<?>> function = invocation.getArgument(0);
              return function.apply(sessionMock);
            });

    dao = new DefaultGameDao(providerMock);
  }

  @Test
  void findById_validId_returnsEntity() {
    var id = UUID.randomUUID();
    var expectedEntity = mock(Game.class);

    when(sessionMock.find(Game.class, id)).thenReturn(Uni.createFrom().item(expectedEntity));

    var actualEntity = dao.findById(id).await().indefinitely();

    assertThat(actualEntity).isSameAs(expectedEntity);
    verify(sessionMock).find(Game.class, id);
  }

  @Test
  void persist_validEntity_callsSessionPersist() {
    var entity = mock(Game.class);

    when(sessionMock.persist(any())).thenReturn(Uni.createFrom().voidItem());

    dao.persist(entity);
  }
}
