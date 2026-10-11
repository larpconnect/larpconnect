package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
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

  private final UUID tenantId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final UUID itemId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<CollectionCommand> createBehavior(CollectionDAO collectionDao) {
    return Behaviors.setup(context -> new CollectionActor(context, collectionDao));
  }

  private Collection sampleCollection() {
    return new Collection(
        collectionId,
        ownerId,
        "Featured Cast",
        Optional.of("Primary cast"),
        1,
        ImmutableList.of(itemId),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("onCreateCollection returns Success on valid payload")
  void onCreateCollection_valid_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.existsByOwnerAndName(tenantId, ownerId, "Featured Cast", Optional.empty()))
        .thenReturn(false);
    when(collectionDao.create(tenantId, ownerId, "Featured Cast", Optional.of("Primary cast")))
        .thenReturn(sampleCollection());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request =
        new CreateCollectionRequest(ownerId, "Featured Cast", Optional.of("Primary cast"));
    testKit.run(new CollectionCommand.CreateCollection(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().id()).isEqualTo(collectionId);
    assertThat(success.collection().name()).isEqualTo("Featured Cast");
  }

  @Test
  @DisplayName("onCreateCollection returns Conflict on duplicate name for owner")
  void onCreateCollection_duplicateName_returnsConflict() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.existsByOwnerAndName(tenantId, ownerId, "Featured Cast", Optional.empty()))
        .thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new CreateCollectionRequest(ownerId, "Featured Cast");
    testKit.run(new CollectionCommand.CreateCollection(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onCreateCollection returns BadRequest on blank name")
  void onCreateCollection_blankName_returnsBadRequest() {
    var collectionDao = mock(CollectionDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new CreateCollectionRequest(ownerId, "   ");
    testKit.run(new CollectionCommand.CreateCollection(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateCollection returns BadRequest on foreign key violation")
  void onCreateCollection_constraintViolation_returnsBadRequest() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.existsByOwnerAndName(any(), any(), any(), any())).thenReturn(false);
    when(collectionDao.create(any(), any(), any(), any()))
        .thenThrow(
            new ConstraintViolationException(
                "FK violation", new SQLException("FK"), "fk_collections_owner"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new CreateCollectionRequest(ownerId, "Featured Cast");
    testKit.run(new CollectionCommand.CreateCollection(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onGetCollection returns Success for compressed form")
  void onGetCollection_compressed_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId))
        .thenReturn(Optional.of(sampleCollection()));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, collectionId, Optional.of("compressed"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().orderedItems()).containsExactly(itemId);
  }

  @Test
  @DisplayName("onGetCollection returns Success for expanded form")
  void onGetCollection_expanded_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId))
        .thenReturn(Optional.of(sampleCollection()));
    var expandedEntity = new ExpandedEntity(itemId, "Individual", "Alice", "Lead");
    when(collectionDao.findExpandedItems(tenantId, collectionId))
        .thenReturn(ImmutableList.of(expandedEntity));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, collectionId, Optional.of("expanded"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().orderedItems()).containsExactly(expandedEntity);
  }

  @Test
  @DisplayName("onGetCollection returns NotFound when absent")
  void onGetCollection_absent_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, collectionId, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchCollection returns Success on valid update")
  void onPatchCollection_valid_returnsSuccess() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId))
        .thenReturn(Optional.of(sampleCollection()));
    when(collectionDao.existsByOwnerAndName(eq(tenantId), eq(ownerId), eq("New Name"), any()))
        .thenReturn(false);
    var updated =
        new Collection(
            collectionId,
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
            eq(collectionId),
            eq(Optional.of("New Name")),
            eq(Optional.of("New summary"))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("New Name", "New summary");
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId, collectionId, request, Optional.of("name,summary"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Success.class);
    var success = (CollectionActorResponse.Success) response;
    assertThat(success.collection().name()).isEqualTo("New Name");
  }

  @Test
  @DisplayName("onPatchCollection returns Conflict on duplicate name")
  void onPatchCollection_duplicateName_returnsConflict() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId))
        .thenReturn(Optional.of(sampleCollection()));
    when(collectionDao.existsByOwnerAndName(eq(tenantId), eq(ownerId), eq("New Name"), any()))
        .thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("New Name", null);
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId, collectionId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onDeleteCollection returns NoContent on success")
  void onDeleteCollection_success_returnsDeleted() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.softDelete(tenantId, collectionId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(new CollectionCommand.DeleteCollection(tenantId, collectionId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.NoContent.class);
  }

  @Test
  @DisplayName("onDeleteCollection returns NotFound when not found")
  void onDeleteCollection_notFound_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.softDelete(tenantId, collectionId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(new CollectionCommand.DeleteCollection(tenantId, collectionId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onAddItem returns NoContent on success")
  void onAddItem_success_returnsItemMutated() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.addItem(eq(tenantId), eq(collectionId), eq(itemId), any(), any()))
        .thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            collectionId,
            CollectionCommand.ItemAction.ADD,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.NoContent.class);
  }

  @Test
  @DisplayName("onAddItem returns NotFound when collection or target missing")
  void onAddItem_missing_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.addItem(eq(tenantId), eq(collectionId), eq(itemId), any(), any()))
        .thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            collectionId,
            CollectionCommand.ItemAction.ADD,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onRemoveItem returns NoContent on success")
  void onRemoveItem_success_returnsItemMutated() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.removeItem(tenantId, collectionId, itemId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            collectionId,
            CollectionCommand.ItemAction.REMOVE,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.NoContent.class);
  }

  @Test
  @DisplayName("onRemoveItem returns NotFound when collection missing")
  void onRemoveItem_missingCollection_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.removeItem(tenantId, collectionId, itemId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.MutateItem(
            tenantId,
            collectionId,
            CollectionCommand.ItemAction.REMOVE,
            itemId,
            Optional.empty(),
            Optional.empty(),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("DefaultCollectionActorFactory creates Behavior")
  void factory_createsBehavior() {
    var collectionDao = mock(CollectionDAO.class);
    var factory = new DefaultCollectionActorFactory(collectionDao);
    assertThat(factory.create()).isNotNull();
  }
}
