package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.ServerDAO;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerAdminActorFactoryTest {

  @Test
  @DisplayName("DefaultServerAdminActorFactory constructor throws NPE when arguments are null")
  void constructor_nullArguments_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultServerAdminActorFactory(null))
        .withMessage("serverDao must not be null");
  }

  @Test
  @DisplayName("DefaultServerAdminActorFactory.create produces executable behavior")
  void create_whenCalled_producesExecutableBehavior() {
    var serverDao = mock(ServerDAO.class);
    when(serverDao.list()).thenReturn(ImmutableList.of());

    var factory = new DefaultServerAdminActorFactory(serverDao);
    var behavior = factory.create();

    assertThat(behavior).isNotNull();

    var testKit = BehaviorTestKit.create(behavior);
    TestInbox<ServerAdminResponse> inbox = TestInbox.create();
    testKit.run(new ServerAdminCommand.ListServers(inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(ServerAdminResponse.ServerList.class);
  }
}
