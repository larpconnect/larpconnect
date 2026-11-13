package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.api.studios.common.ExpandedItemResponse;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.Collection;
import com.larpconnect.njall.data.domain.ExpandedEntity;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionActorTest {

  private static final String STUDIO_IDENTIFIER = "valiant";

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final UUID itemId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<CollectionCommand> createBehavior(CollectionDAO collectionDao) {
    return Behaviors.setup(context -> new CollectionActor(context, collectionDao));
  }

  private Collection sampleCollection(UUID effectiveOwnerId) {
    return new Collection(
        collectionId,
        actorId,
        effectiveOwnerId,
        "Featured Cast",
        Optional.of("Primary cast"),
        1,
        ImmutableList.of(itemId),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("onCreateCollection defaults ownerId to path actorId when omitted")
  void onCreateCollection_omittedOwnerId_defaultsToActorId() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
    when(collectionDao.create(
            tenantId, actorId, actorId, "Featured Cast", Optional.of("Primary cast")))
        .thenReturn(sampleCollection(actorId));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request =
        new CreateCollectionRequest(Optional.empty(), "Featured Cast", Optional.of("Primary cast"));
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().id()).isEqualTo(collectionId);
    assertThat(success.collection().ownerId()).isEqualTo(actorId);
    assertThat(success.collection().generator())
        .isEqualTo("/api/studios/valiant/v1/actors/" + actorId);
  }

  @Test
  @DisplayName("onCreateCollection supports explicit active non-Actor entity ownerId")
  void onCreateCollection_explicitEntityOwner_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
    var created =
        new Collection(
            collectionId,
            actorId,
            ownerId,
            "Character Gallery",
            Optional.empty(),
            0,
            ImmutableList.of(),
            now,
            now,
            Optional.empty());
    when(collectionDao.create(tenantId, actorId, ownerId, "Character Gallery", Optional.empty()))
        .thenReturn(created);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request =
        new CreateCollectionRequest(Optional.of(ownerId), "Character Gallery", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().ownerId()).isEqualTo(ownerId);
    assertThat(success.collection().generator())
        .isEqualTo("/api/studios/valiant/v1/actors/" + actorId);
  }

  @Test
  @DisplayName("onCreateCollection returns BadRequest when explicit ownerId is missing or deleted")
  void onCreateCollection_missingOwnerEntity_returnsBadRequest() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
    when(collectionDao.create(tenantId, actorId, ownerId, "Featured Cast", Optional.empty()))
        .thenThrow(new IllegalArgumentException("Owner entity not found or soft-deleted"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request =
        new CreateCollectionRequest(Optional.of(ownerId), "Featured Cast", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateCollection returns Conflict on duplicate name for owner")
  void onCreateCollection_duplicateName_returnsConflict() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
    when(collectionDao.create(tenantId, actorId, ownerId, "Featured Cast", Optional.empty()))
        .thenThrow(
            new IllegalStateException(
                "Collection with name 'Featured Cast' already exists for owner"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request =
        new CreateCollectionRequest(Optional.of(ownerId), "Featured Cast", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onCreateCollection returns BadRequest on blank name")
  void onCreateCollection_blankName_returnsBadRequest() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new CreateCollectionRequest(Optional.of(ownerId), "   ", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateCollection returns BadRequest on foreign key violation")
  void onCreateCollection_constraintViolation_returnsBadRequest() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
    when(collectionDao.create(any(), any(), any(), any(), any()))
        .thenThrow(
            new ConstraintViolationException(
                "FK violation", new SQLException("FK"), "fk_collections_owner"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request =
        new CreateCollectionRequest(Optional.of(ownerId), "Featured Cast", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onGetCollection returns Success for compressed form")
  void onGetCollection_compressed_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, actorId, collectionId))
        .thenReturn(Optional.of(sampleCollection(ownerId)));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            collectionId,
            Optional.of("compressed"),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().orderedItems()).containsExactly(itemId);
  }

  @Test
  @DisplayName("onGetCollection returns Success for expanded form")
  void onGetCollection_expanded_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, actorId, collectionId))
        .thenReturn(Optional.of(sampleCollection(ownerId)));
    var expandedEntity = new ExpandedEntity(itemId, actorId, "Individual", "Alice", "Lead");
    when(collectionDao.findExpandedItems(tenantId, actorId, collectionId))
        .thenReturn(ImmutableList.of(expandedEntity));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            collectionId,
            Optional.of("expanded"),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().orderedItems()).hasSize(1);
    var item = (ExpandedItemResponse) success.collection().orderedItems().get(0);
    assertThat(item.id()).isEqualTo(itemId);
    assertThat(item.generator()).isEqualTo("/api/studios/valiant/v1/actors/" + actorId);
  }

  @Test
  @DisplayName("onGetCollection returns NotFound when absent or actor mismatch")
  void onGetCollection_absent_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, actorId, collectionId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, collectionId, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchCollection returns Success on valid update")
  void onPatchCollection_valid_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    var updated =
        new Collection(
            collectionId,
            actorId,
            ownerId,
            "New Name",
            Optional.of("New summary"),
            1,
            ImmutableList.of(itemId),
            now,
            now,
            Optional.empty());
    when(collectionDao.patch(
            eq(tenantId),
            eq(actorId),
            eq(collectionId),
            eq(Optional.of("New Name")),
            eq(Optional.of("New summary"))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("New Name", "New summary");
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            collectionId,
            request,
            Optional.of("name,summary"),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().name()).isEqualTo("New Name");
  }

  @Test
  @DisplayName("onPatchCollection returns Conflict on duplicate name")
  void onPatchCollection_duplicateName_returnsConflict() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.patch(
            tenantId, actorId, collectionId, Optional.of("New Name"), Optional.empty()))
        .thenThrow(
            new IllegalStateException("Collection with name 'New Name' already exists for owner"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("New Name", null);
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            collectionId,
            request,
            Optional.empty(),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onDeleteCollection returns NoContent on success and NotFound on mismatch")
  void onDeleteCollection_handlesSuccessAndNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.softDelete(tenantId, actorId, collectionId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.DeleteCollection(tenantId, actorId, collectionId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CollectionActorResponse.NoContent.class);

    var otherActorId = UUID.randomUUID();
    when(collectionDao.softDelete(tenantId, otherActorId, collectionId)).thenReturn(false);
    testKit.run(
        new CollectionCommand.DeleteCollection(
            tenantId, otherActorId, collectionId, inbox.getRef()));
    var notFound = inbox.receiveMessage();
    assertThat(notFound).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) notFound).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onMutateItem ADD returns NoContent on success and NotFound on mismatch")
  void onAddItem_handlesSuccessAndNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.addItem(
            eq(tenantId), eq(actorId), eq(collectionId), eq(itemId), any(), any()))
        .thenReturn(true);
    var otherActorId = UUID.randomUUID();
    when(collectionDao.addItem(
            eq(tenantId), eq(otherActorId), eq(collectionId), eq(itemId), any(), any()))
        .thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            actorId,
            collectionId,
            CollectionCommand.ItemAction.ADD,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CollectionActorResponse.NoContent.class);

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            otherActorId,
            collectionId,
            CollectionCommand.ItemAction.ADD,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));
    assertThat(((CollectionActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onMutateItem REMOVE returns NoContent on success and NotFound on mismatch")
  void onRemoveItem_handlesSuccessAndNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.removeItem(tenantId, actorId, collectionId, itemId)).thenReturn(true);
    var otherActorId = UUID.randomUUID();
    when(collectionDao.removeItem(tenantId, otherActorId, collectionId, itemId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            actorId,
            collectionId,
            CollectionCommand.ItemAction.REMOVE,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CollectionActorResponse.NoContent.class);

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            otherActorId,
            collectionId,
            CollectionCommand.ItemAction.REMOVE,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));
    assertThat(((CollectionActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("DefaultCollectionActorFactory creates working CollectionActor behavior")
  void factory_createsBehavior() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, actorId, collectionId)).thenReturn(Optional.empty());
    var factory = new DefaultCollectionActorFactory(collectionDao);

    var testKit = BehaviorTestKit.create(factory.create());
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();
    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, collectionId, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }
}
