package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.entity.User;
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
class DefaultUserDaoTest {

  @Mock private Provider<Mutiny.SessionFactory> sessionFactoryProvider;
  @Mock private Mutiny.SessionFactory sessionFactory;
  @Mock private Mutiny.Session session;

  private DefaultUserDao userDao;

  @BeforeEach
  void setUp() {
    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    userDao = new DefaultUserDao(sessionFactoryProvider);
  }

  @Test
  void findById_shouldReturnUser_whenFound() {
    UUID id = UUID.randomUUID();
    User expectedUser = new User();
    expectedUser.setId(id);

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<User>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.find(User.class, id)).thenReturn(Uni.createFrom().item(expectedUser));

    User actualUser = userDao.findById(id).await().indefinitely();

    assertThat(actualUser).isEqualTo(expectedUser);
    verify(session).find(User.class, id);
  }

  @Test
  void persist_shouldReturnPersistedUser() {
    User userToPersist = new User();

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {
              Function<Mutiny.Session, Uni<User>> function = invocation.getArgument(0);
              return function.apply(session);
            });
    when(session.persist(userToPersist)).thenReturn(Uni.createFrom().voidItem());
    when(session.flush()).thenReturn(Uni.createFrom().voidItem());

    User persistedUser = userDao.persist(userToPersist).await().indefinitely();

    assertThat(persistedUser).isEqualTo(userToPersist);
    verify(session).persist(userToPersist);
    verify(session).flush();
  }
}
