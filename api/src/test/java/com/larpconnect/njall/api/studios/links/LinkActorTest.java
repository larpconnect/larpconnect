package com.larpconnect.njall.api.studios.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.LinkDAO;
import com.larpconnect.njall.data.domain.Link;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LinkActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID linkId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<LinkCommand> createBehavior(LinkDAO linkDao) {
    return Behaviors.setup(context -> new LinkActor(context, linkDao));
  }

  private Link sampleLink() {
    return new Link(
        linkId,
        "website",
        "https://valiant.example.com",
        "text/html",
        "Official Studio",
        now,
        now,
        null);
  }

  @Test
  @DisplayName("onCreateLink returns Success on valid payload")
  void onCreateLink_valid_returnsSuccess() {
    var linkDao = mock(LinkDAO.class);
    var domain = sampleLink();
    when(linkDao.create(
            eq(tenantId),
            eq("website"),
            eq("https://valiant.example.com"),
            eq("text/html"),
            eq(Optional.of("Official Studio"))))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request =
        new CreateLinkRequest(
            "website", "https://valiant.example.com", "text/html", "Official Studio");
    testKit.run(new LinkCommand.CreateLink(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Success.class);
    var success = (LinkActorResponse.Success) response;
    assertThat(success.link().id()).isEqualTo(linkId);
    assertThat(success.link().linkType()).isEqualTo("website");
  }

  @Test
  @DisplayName("onCreateLink returns BadRequest on invalid payload")
  void onCreateLink_invalid_returnsBadRequest() {
    var linkDao = mock(LinkDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request = new CreateLinkRequest("website", "invalid-uri", "text/html", "summary");
    testKit.run(new LinkCommand.CreateLink(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onCreateLink returns Failure when DAO throws exception")
  void onCreateLink_exception_returnsFailure() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.create(any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Database error"));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request = new CreateLinkRequest("website", "https://example.com");
    testKit.run(new LinkCommand.CreateLink(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Failure.class);
    assertThat(((LinkActorResponse.Failure) response).message()).isEqualTo("Database error");
  }

  @Test
  @DisplayName("onGetLink returns Success when link exists")
  void onGetLink_found_returnsSuccess() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.findById(tenantId, linkId)).thenReturn(Optional.of(sampleLink()));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.GetLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Success.class);
    assertThat(((LinkActorResponse.Success) response).link().id()).isEqualTo(linkId);
  }

  @Test
  @DisplayName("onGetLink returns NotFound when link does not exist")
  void onGetLink_notFound_returnsNotFound() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.findById(tenantId, linkId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.GetLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onGetLink returns Failure when DAO throws exception")
  void onGetLink_exception_returnsFailure() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.findById(tenantId, linkId)).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.GetLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Failure.class);
  }

  @Test
  @DisplayName("onPatchLink applies update_mask and returns Success")
  void onPatchLink_withMask_returnsSuccess() {
    var linkDao = mock(LinkDAO.class);
    var updated =
        new Link(
            linkId,
            "website",
            "https://updated.example.com",
            "text/html",
            "Updated",
            now,
            now,
            null);
    when(linkDao.patch(
            eq(tenantId),
            eq(linkId),
            eq(Optional.empty()),
            eq(Optional.of("https://updated.example.com")),
            eq(Optional.empty()),
            eq(Optional.of("Updated"))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request =
        new UpdateLinkRequest(
            "ignoredType", "https://updated.example.com", "ignoredMedia", "Updated");
    testKit.run(
        new LinkCommand.PatchLink(
            tenantId, linkId, request, Optional.of("url,summary"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Success.class);
    assertThat(((LinkActorResponse.Success) response).link().url())
        .isEqualTo("https://updated.example.com");
  }

  @Test
  @DisplayName("onPatchLink without mask updates all present fields")
  void onPatchLink_withoutMask_returnsSuccess() {
    var linkDao = mock(LinkDAO.class);
    var updated =
        new Link(
            linkId,
            "discord",
            "https://discord.gg/valiant",
            "text/html",
            "Discord",
            now,
            now,
            null);
    when(linkDao.patch(
            eq(tenantId),
            eq(linkId),
            eq(Optional.of("discord")),
            eq(Optional.of("https://discord.gg/valiant")),
            eq(Optional.empty()),
            eq(Optional.empty())))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request = new UpdateLinkRequest("discord", "https://discord.gg/valiant", null, null);
    testKit.run(
        new LinkCommand.PatchLink(tenantId, linkId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchLink returns BadRequest on invalid update payload")
  void onPatchLink_invalid_returnsBadRequest() {
    var linkDao = mock(LinkDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request = new UpdateLinkRequest(null, "invalid-url", null, null);
    testKit.run(
        new LinkCommand.PatchLink(tenantId, linkId, request, Optional.of("url"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.BadRequest.class);
  }

  @Test
  @DisplayName("onPatchLink returns NotFound when link is not found in DAO")
  void onPatchLink_notFound_returnsNotFound() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.patch(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request = new UpdateLinkRequest("website", "https://example.com", null, null);
    testKit.run(
        new LinkCommand.PatchLink(tenantId, linkId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onPatchLink returns Failure when DAO throws exception")
  void onPatchLink_exception_returnsFailure() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.patch(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Patch failed"));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request = new UpdateLinkRequest("website", "https://example.com", null, null);
    testKit.run(
        new LinkCommand.PatchLink(tenantId, linkId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Failure.class);
  }

  @Test
  @DisplayName("onDeleteLink returns Deleted when soft delete succeeds")
  void onDeleteLink_success_returnsDeleted() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.softDelete(tenantId, linkId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.DeleteLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Deleted.class);
  }

  @Test
  @DisplayName("onDeleteLink returns NotFound when soft delete returns false")
  void onDeleteLink_notFound_returnsNotFound() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.softDelete(tenantId, linkId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.DeleteLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.NotFound.class);
  }

  @Test
  @DisplayName("onDeleteLink returns Failure when DAO throws exception")
  void onDeleteLink_exception_returnsFailure() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.softDelete(tenantId, linkId)).thenThrow(new RuntimeException("Delete failed"));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.DeleteLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Failure.class);
  }

  @Test
  @DisplayName("onCreateLink defaults mediaType to text/html when mediaType is blank")
  void onCreateLink_blankMediaType_defaults() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.create(
            eq(tenantId), eq("website"), eq("https://example.com"), eq("text/html"), any()))
        .thenReturn(sampleLink());

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    var request =
        new CreateLinkRequest("website", "https://example.com", Optional.empty(), Optional.empty());
    testKit.run(new LinkCommand.CreateLink(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Success.class);
  }

  @Test
  @DisplayName("handleError falls back to default reason when exception message is null")
  void handleError_nullMessage_usesDefault() {
    var linkDao = mock(LinkDAO.class);
    when(linkDao.findById(tenantId, linkId)).thenThrow(new RuntimeException((String) null));

    var testKit = BehaviorTestKit.create(createBehavior(linkDao));
    TestInbox<LinkActorResponse> inbox = TestInbox.create();

    testKit.run(new LinkCommand.GetLink(tenantId, linkId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(LinkActorResponse.Failure.class);
    assertThat(((LinkActorResponse.Failure) response).message())
        .isEqualTo("Error executing get link");
  }
}
