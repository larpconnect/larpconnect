package com.larpconnect.njall.data.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ActiveSessionFactoriesTest {

  @Test
  @DisplayName("register tracks active factories")
  void register_validFactory_tracksActiveFactories() {
    var tracker = new ActiveSessionFactories();
    var factory = mock(SessionFactory.class);

    tracker.register(factory);
    assertThat(tracker.activeCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("closeAll closes open factories and skips already closed factories")
  void closeAll_openAndClosedFactories_closesOpenAndClears() {
    var tracker = new ActiveSessionFactories();
    var openFactory = mock(SessionFactory.class);
    var closedFactory = mock(SessionFactory.class);

    when(openFactory.isClosed()).thenReturn(false);
    when(closedFactory.isClosed()).thenReturn(true);

    tracker.register(openFactory);
    tracker.register(closedFactory);
    assertThat(tracker.activeCount()).isEqualTo(2);

    tracker.closeAll();

    verify(openFactory).close();
    verify(closedFactory, never()).close();
    assertThat(tracker.activeCount()).isEqualTo(0);
  }
}
