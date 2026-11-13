package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionActorErrorHandlingTest {

  private static final String STUDIO_IDENTIFIER = "valiant";

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID itemId = UUID.randomUUID();

  private Behavior<CollectionCommand> createBehavior(CollectionDAO collectionDao) {
    return Behaviors.setup(context -> new CollectionActor(context, collectionDao));
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
  @DisplayName("onCreateCollection returns 404 when path actorId is not active actor")
  void onCreateCollection_inactiveOrNonActor_returnsNotFoundBeforeValidation() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var blankRequest = new CreateCollectionRequest(Optional.empty(), "   ", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, blankRequest, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
    verify(collectionDao, never()).create(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("onCreateCollection returns Failure on DAO exception")
  void onCreateCollection_exception_returnsFailure() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.isActiveActor(tenantId, actorId))
        .thenThrow(new RuntimeException("DB offline"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    var request = new CreateCollectionRequest(Optional.empty(), "Featured Cast", Optional.empty());
    testKit.run(
        new CollectionCommand.CreateCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onGetCollection returns Failure on DAO exception")
  void onGetCollection_exception_returnsFailure() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.findById(tenantId, actorId, collectionId))
        .thenThrow(new RuntimeException("DB error"));

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.GetCollection(
            tenantId, STUDIO_IDENTIFIER, actorId, collectionId, Optional.empty(), inbox.getRef()));

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
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            collectionId,
            request,
            Optional.empty(),
            inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchCollection returns NotFound when DAO patch returns empty")
  void onPatchCollection_daoPatchEmpty_returnsNotFound() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.patch(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

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
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onDeleteCollection returns Failure when exception has no message")
  void onDeleteCollection_exceptionWithoutMessage_returnsFailure() {
    var collectionDao = mock(CollectionDAO.class);
    when(collectionDao.softDelete(tenantId, actorId, collectionId))
        .thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(collectionDao));
    TestInbox<CollectionActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CollectionCommand.DeleteCollection(tenantId, actorId, collectionId, inbox.getRef()));

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
    when(collectionDao.addItem(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Mutation failed"));

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

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(CollectionActorResponse.Failure.class);
    assertThat(((CollectionActorResponse.Failure) response).status()).isEqualTo(500);
  }
}
