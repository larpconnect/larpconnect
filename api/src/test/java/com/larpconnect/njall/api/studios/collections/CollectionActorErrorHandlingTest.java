package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.Collection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionActorErrorHandlingTest {

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
  @DisplayName("isConstraintViolation covers all matching and non-matching conditions")
  void isConstraintViolation_conditions() {
    assertThat(CollectionActor.isConstraintViolation(null)).isFalse();
    assertThat(CollectionActor.isConstraintViolation(new RuntimeException())).isFalse();
    assertThat(CollectionActor.isConstraintViolation(new RuntimeException("generic error")))
        .isFalse();
    assertThat(
            CollectionActor.isConstraintViolation(
                new RuntimeException(new SQLException("violates foreign key constraint"))))
        .isTrue();
    assertThat(
            CollectionActor.isConstraintViolation(
                new RuntimeException("violates unique constraint")))
        .isTrue();
    assertThat(
            CollectionActor.isConstraintViolation(
                new RuntimeException("violates check constraint")))
        .isTrue();
  }

  @Test
  @DisplayName("onGetCollection returns Failure on DAO exception")
  void onGetCollection_exception_returnsFailure() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId))
        .thenThrow(new RuntimeException("DB error"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, collectionId, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onPatchCollection returns BadRequest on validation error")
  void onPatchCollection_validationError_returnsBadRequest() {
    var collectionDao = mock(CollectionDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("   ", null);
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId, collectionId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchCollection returns NotFound when collection missing during uniqueness check")
  void onPatchCollection_missingCollection_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("New Name", null);
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId, collectionId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchCollection returns NotFound when DAO patch returns empty")
  void onPatchCollection_daoPatchEmpty_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, collectionId))
        .thenReturn(Optional.of(sampleCollection()));
    when(collectionDao.existsByOwnerAndName(any(), any(), any(), any())).thenReturn(false);
    when(collectionDao.patch(any(), any(), any(), any())).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new UpdateCollectionRequest("New Name", null);
    testKit.run(
        new CollectionCommand.PatchCollection(
            tenantId, collectionId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onDeleteCollection returns Failure when exception has no message")
  void onDeleteCollection_exceptionWithoutMessage_returnsFailure() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.softDelete(tenantId, collectionId)).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(new CollectionCommand.DeleteCollection(tenantId, collectionId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    var failure = (CollectionActorResponse.Failure) response;
    assertThat(failure.status()).isEqualTo(500);
    assertThat(failure.message()).isEqualTo("Error executing delete collection");
  }

  @Test
  @DisplayName("onMutateItem returns Failure on unexpected exception")
  void onMutateItem_unexpectedException_returnsFailure() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.addItem(any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Mutation failed"));

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
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(500);
  }
}
