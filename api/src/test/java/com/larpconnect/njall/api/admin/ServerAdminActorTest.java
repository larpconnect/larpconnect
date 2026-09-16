package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.ServerDAO;
import com.larpconnect.njall.data.domain.ContactType;
import com.larpconnect.njall.data.domain.RoleType;
import com.larpconnect.njall.data.domain.Server;
import com.larpconnect.njall.data.domain.ServerContact;
import java.time.Instant;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerAdminActorTest {

  private static Behavior<ServerAdminCommand> createBehavior(ServerDAO serverDao) {
    return Behaviors.setup(context -> new ServerAdminActor(context, serverDao));
  }

  private static Server sampleServer() {
    var contact =
        ServerContact.of(
            UUID.randomUUID(), RoleType.ADMIN, ContactType.EMAIL, "admin@example.com", 0);
    return Server.of(
        UUID.randomUUID(),
        "Test Server",
        "test.larpconnect.com",
        Instant.now(),
        ImmutableList.of(contact));
  }

  @Test
  @DisplayName("ServerAdminActor responds with ServerList when DAO returns servers")
  void onListServers_daoReturnsServers_emitsSuccessResponse() {
    var serverDao = mock(ServerDAO.class);
    var server = sampleServer();
    when(serverDao.list()).thenReturn(ImmutableList.of(server));

    var testKit = BehaviorTestKit.create(createBehavior(serverDao));
    TestInbox<ServerAdminResponse> inbox = TestInbox.create();

    testKit.run(new ServerAdminCommand.ListServers(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(ServerAdminResponse.ServerList.class);
    var list = (ServerAdminResponse.ServerList) response;
    assertThat(list.servers()).containsExactly(server);
  }

  @Test
  @DisplayName("ServerAdminActor responds with Failure when DAO throws exception with message")
  void onListServers_daoThrowsException_emitsFailureResponse() {
    var serverDao = mock(ServerDAO.class);
    when(serverDao.list()).thenThrow(new RuntimeException("Database offline"));

    var testKit = BehaviorTestKit.create(createBehavior(serverDao));
    TestInbox<ServerAdminResponse> inbox = TestInbox.create();

    testKit.run(new ServerAdminCommand.ListServers(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(ServerAdminResponse.Failure.class);
    var failure = (ServerAdminResponse.Failure) response;
    assertThat(failure.reason()).contains("Database offline");
  }

  @Test
  @DisplayName("ServerAdminActor responds with Failure when DAO throws exception without message")
  void onListServers_daoThrowsExceptionWithoutMessage_emitsFailureResponse() {
    var serverDao = mock(ServerDAO.class);
    when(serverDao.list()).thenThrow(new RuntimeException((String) null));

    var testKit = BehaviorTestKit.create(createBehavior(serverDao));
    TestInbox<ServerAdminResponse> inbox = TestInbox.create();

    testKit.run(new ServerAdminCommand.ListServers(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(ServerAdminResponse.Failure.class);
    var failure = (ServerAdminResponse.Failure) response;
    assertThat(failure.reason()).isEqualTo("Error querying servers");
  }

  @Test
  @DisplayName("ServerAdminActor constructor throws NullPointerException when arguments are null")
  void constructor_nullArguments_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                BehaviorTestKit.create(
                    Behaviors.<ServerAdminCommand>setup(
                        context -> new ServerAdminActor(context, null))))
        .withMessage("serverDao must not be null");
  }
}
