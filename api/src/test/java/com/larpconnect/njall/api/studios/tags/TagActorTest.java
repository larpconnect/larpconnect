package com.larpconnect.njall.api.studios.tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import com.larpconnect.njall.data.domain.Hashtag;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TagActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID tagId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<TagCommand> createBehavior(HashtagDAO hashtagDao) {
    return Behaviors.setup(context -> new TagActor(context, hashtagDao));
  }

  private Hashtag sampleTag() {
    return new Hashtag(
        tagId,
        "SolarPunk",
        "hashtag",
        "/api/studios/valiant/v1/tags/SolarPunk",
        "application/json",
        Optional.of("Eco-futuristic"),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("onCreateTags creates new tag when not existing")
  void onCreateTags_newTag_returnsSuccessCreated() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.findByTag(tenantId, "SolarPunk")).thenReturn(Optional.empty());
    when(hashtagDao.create(
            eq(tenantId),
            eq("SolarPunk"),
            eq("/api/studios/valiant/v1/tags/SolarPunk"),
            eq(Optional.of("Eco-futuristic"))))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new CreateTagRequest("#SolarPunk", Optional.of("Eco-futuristic"));
    testKit.run(
        new TagCommand.CreateTags(
            tenantId,
            "/api/studios/valiant/v1/tags",
            ImmutableList.of(request),
            false,
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Success.class);
    var success = (TagActorResponse.Success) response;
    assertThat(success.created()).isTrue();
    assertThat(success.tag().id()).isEqualTo(tagId);
    assertThat(success.tag().tag()).isEqualTo("SolarPunk");
  }

  @Test
  @DisplayName("onCreateTags returns existing tag when already exists (idempotency)")
  void onCreateTags_existingTag_returnsSuccessNotCreated() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.findByTag(tenantId, "SolarPunk")).thenReturn(Optional.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new CreateTagRequest("SolarPunk", Optional.of("Different"));
    testKit.run(
        new TagCommand.CreateTags(
            tenantId,
            "/api/studios/valiant/v1/tags",
            ImmutableList.of(request),
            false,
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Success.class);
    var success = (TagActorResponse.Success) response;
    assertThat(success.created()).isFalse();
    assertThat(success.tag().id()).isEqualTo(tagId);
  }

  @Test
  @DisplayName("onCreateTags returns Error with 400 on invalid tag")
  void onCreateTags_invalid_returnsError400() {
    var hashtagDao = mock(HashtagDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new CreateTagRequest("", Optional.empty());
    testKit.run(
        new TagCommand.CreateTags(
            tenantId,
            "/api/studios/valiant/v1/tags",
            ImmutableList.of(request),
            false,
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateTags returns Error with 500 on DAO exception")
  void onCreateTags_daoException_returnsError500() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findByTag(any(), any())).thenThrow(new RuntimeException("DB down"));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new CreateTagRequest("SolarPunk", Optional.empty());
    testKit.run(
        new TagCommand.CreateTags(
            tenantId,
            "/api/studios/valiant/v1/tags",
            ImmutableList.of(request),
            false,
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onCreateTags with batch=true provisions multiple tags successfully")
  void onCreateTags_batch_returnsItems() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.batchCreate(eq(tenantId), any())).thenReturn(ImmutableList.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var batch = ImmutableList.of(new CreateTagRequest("SolarPunk", Optional.empty()));
    testKit.run(
        new TagCommand.CreateTags(
            tenantId, "/api/studios/valiant/v1/tags", batch, true, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Items.class);
    var items = (TagActorResponse.Items) response;
    assertThat(items.tags()).hasSize(1);
  }

  @Test
  @DisplayName("onCreateTags with batch=true returns Error with 400 on validation failure")
  void onCreateTags_batch_validationError_returnsError400() {
    var hashtagDao = mock(HashtagDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    testKit.run(
        new TagCommand.CreateTags(
            tenantId, "/api/studios/valiant/v1/tags", ImmutableList.of(), true, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onQueryTag resolves by UUID and by tag name")
  void onQueryTag_resolvesDualIdentifiers() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.findById(tenantId, tagId)).thenReturn(Optional.of(domain));
    when(hashtagDao.findByTag(tenantId, "solarpunk")).thenReturn(Optional.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    // Get by UUID
    testKit.run(new TagCommand.QueryTag(tenantId, Optional.of(tagId.toString()), inbox.getRef()));
    var response1 = inbox.receiveMessage();
    assertThat(response1).isInstanceOf(TagActorResponse.Success.class);

    // Get by name
    testKit.run(new TagCommand.QueryTag(tenantId, Optional.of("#solarpunk"), inbox.getRef()));
    var response2 = inbox.receiveMessage();
    assertThat(response2).isInstanceOf(TagActorResponse.Success.class);

    // Get non-existent
    when(hashtagDao.findByTag(tenantId, "unknown")).thenReturn(Optional.empty());
    testKit.run(new TagCommand.QueryTag(tenantId, Optional.of("unknown"), inbox.getRef()));
    var response3 = inbox.receiveMessage();
    assertThat(response3).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response3).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onQueryTag returns Error with 500 on DAO exception and uses fallback message")
  void onQueryTag_daoException_returnsError500() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findById(tenantId, tagId)).thenThrow(new RuntimeException((String) null));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    testKit.run(new TagCommand.QueryTag(tenantId, Optional.of(tagId.toString()), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    var failure = (TagActorResponse.Failure) response;
    assertThat(failure.status()).isEqualTo(500);
    assertThat(failure.message()).isEqualTo("Error executing query tag");
  }

  @Test
  @DisplayName("onQueryTag with empty idOrName returns all active studio hashtags")
  void onQueryTag_emptyId_returnsAll() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.listAll(tenantId)).thenReturn(ImmutableList.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    testKit.run(new TagCommand.QueryTag(tenantId, Optional.empty(), inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Items.class);
    var items = (TagActorResponse.Items) response;
    assertThat(items.tags()).hasSize(1);
  }

  @Test
  @DisplayName("onPatchTag updates fields and respects update_mask")
  void onPatchTag_updatesSuccessfully() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.findById(tenantId, tagId)).thenReturn(Optional.of(domain));
    when(hashtagDao.patch(
            eq(tenantId), eq(tagId), eq(Optional.of("CyberPunk")), eq(Optional.empty())))
        .thenReturn(Optional.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new UpdateTagRequest(Optional.of("CyberPunk"), Optional.of("Ignored summary"));
    testKit.run(
        new TagCommand.PatchTag(
            tenantId, tagId.toString(), request, Optional.of("tag"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchTag updates all fields when update_mask is empty")
  void onPatchTag_noUpdateMask_updatesAllFields() {
    var hashtagDao = mock(HashtagDAO.class);
    var domain = sampleTag();
    when(hashtagDao.findById(tenantId, tagId)).thenReturn(Optional.of(domain));
    when(hashtagDao.patch(
            eq(tenantId), eq(tagId), eq(Optional.of("CyberPunk")), eq(Optional.of("New Summary"))))
        .thenReturn(Optional.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new UpdateTagRequest(Optional.of("CyberPunk"), Optional.of("New Summary"));
    testKit.run(
        new TagCommand.PatchTag(
            tenantId, tagId.toString(), request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchTag returns 400 on validation failure")
  void onPatchTag_validationFailure_returns400() {
    var hashtagDao = mock(HashtagDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new UpdateTagRequest(Optional.empty(), Optional.empty());
    testKit.run(
        new TagCommand.PatchTag(
            tenantId, tagId.toString(), request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchTag returns 404 when tag name not found")
  void onPatchTag_tagNotFound_returns404() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findByTag(tenantId, "unknown")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new UpdateTagRequest(Optional.of("CyberPunk"), Optional.empty());
    testKit.run(
        new TagCommand.PatchTag(tenantId, "unknown", request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchTag returns 404 when DAO patch returns empty")
  void onPatchTag_daoPatchEmpty_returns404() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findById(tenantId, tagId)).thenReturn(Optional.of(sampleTag()));
    when(hashtagDao.patch(eq(tenantId), eq(tagId), any(), any())).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    var request = new UpdateTagRequest(Optional.of("CyberPunk"), Optional.empty());
    testKit.run(
        new TagCommand.PatchTag(
            tenantId, tagId.toString(), request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onDeleteTag soft deletes existing tag")
  void onDeleteTag_softDeletes() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findByTag(tenantId, "SolarPunk")).thenReturn(Optional.of(sampleTag()));
    when(hashtagDao.softDelete(tenantId, tagId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    testKit.run(new TagCommand.DeleteTag(tenantId, "#SolarPunk", inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Deleted.class);
  }

  @Test
  @DisplayName("onDeleteTag returns 404 when tag name not found")
  void onDeleteTag_tagNotFound_returns404() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findByTag(tenantId, "unknown")).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    testKit.run(new TagCommand.DeleteTag(tenantId, "unknown", inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onDeleteTag returns 404 when DAO softDelete returns false")
  void onDeleteTag_softDeleteReturnsFalse_returns404() {
    var hashtagDao = mock(HashtagDAO.class);
    when(hashtagDao.findByTag(tenantId, "SolarPunk")).thenReturn(Optional.of(sampleTag()));
    when(hashtagDao.softDelete(tenantId, tagId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(hashtagDao));
    TestInbox<TagActorResponse> inbox = TestInbox.create();

    testKit.run(new TagCommand.DeleteTag(tenantId, "SolarPunk", inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(TagActorResponse.Failure.class);
    assertThat(((TagActorResponse.Failure) response).status()).isEqualTo(404);
  }
}
