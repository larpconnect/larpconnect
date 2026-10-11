package com.larpconnect.njall.api.studios.individuals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
import com.larpconnect.njall.data.domain.Individual;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class IndividualActorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID individualId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private Behavior<IndividualCommand> createBehavior(IndividualDAO individualDao) {
    return Behaviors.setup(context -> new IndividualActor(context, individualDao));
  }

  private Individual sampleIndividual() {
    return new Individual(individualId, "Jane Eyre", "Visiting scholar", now, now, null);
  }

  @Test
  @DisplayName("onCreateIndividual returns Success on valid payload")
  void onCreateIndividual_valid_returnsSuccess() {
    var individualDao = mock(IndividualDAO.class);
    var domain = sampleIndividual();
    when(individualDao.create(eq(tenantId), eq("Jane Eyre"), eq(Optional.of("Visiting scholar"))))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new CreateIndividualRequest("Jane Eyre", Optional.of("Visiting scholar"));
    testKit.run(new IndividualCommand.CreateIndividual(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Success.class);
    var success = (IndividualActorResponse.Success) response;
    assertThat(success.individual().id()).isEqualTo(individualId);
    assertThat(success.individual().name()).isEqualTo("Jane Eyre");
  }

  @Test
  @DisplayName("onCreateIndividual returns BadRequest on blank name")
  void onCreateIndividual_blankName_returnsBadRequest() {
    var individualDao = mock(IndividualDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new CreateIndividualRequest("");
    testKit.run(new IndividualCommand.CreateIndividual(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateIndividual returns Failure on DAO exception")
  void onCreateIndividual_daoException_returnsFailure() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.create(any(), any(), any())).thenThrow(new RuntimeException("DB error"));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new CreateIndividualRequest("Jane Eyre");
    testKit.run(new IndividualCommand.CreateIndividual(tenantId, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onGetIndividual returns Success when found")
  void onGetIndividual_found_returnsSuccess() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.findById(tenantId, individualId))
        .thenReturn(Optional.of(sampleIndividual()));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.GetIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Success.class);
    var success = (IndividualActorResponse.Success) response;
    assertThat(success.individual().id()).isEqualTo(individualId);
  }

  @Test
  @DisplayName("onGetIndividual returns NotFound when absent")
  void onGetIndividual_absent_returnsNotFound() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.findById(tenantId, individualId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.GetIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onGetIndividual returns Failure on exception")
  void onGetIndividual_exception_returnsFailure() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.findById(any(), any())).thenThrow(new RuntimeException("Lookup error"));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.GetIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onPatchIndividual applies updates and returns Success")
  void onPatchIndividual_success() {
    var individualDao = mock(IndividualDAO.class);
    var updated = new Individual(individualId, "Jane Rochester", "Updated bio", now, now, null);
    when(individualDao.patch(
            eq(tenantId),
            eq(individualId),
            eq(Optional.of("Jane Rochester")),
            eq(Optional.of("Updated bio"))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new UpdateIndividualRequest("Jane Rochester", "Updated bio");
    testKit.run(
        new IndividualCommand.PatchIndividual(
            tenantId, individualId, request, Optional.of("name,summary"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Success.class);
    var success = (IndividualActorResponse.Success) response;
    assertThat(success.individual().name()).isEqualTo("Jane Rochester");
  }

  @Test
  @DisplayName("onPatchIndividual without updateMask applies non-empty fields")
  void onPatchIndividual_withoutMask() {
    var individualDao = mock(IndividualDAO.class);
    var updated =
        new Individual(individualId, "Jane Rochester", (String) null, now, now, (Instant) null);
    when(individualDao.patch(
            eq(tenantId),
            eq(individualId),
            eq(Optional.of("Jane Rochester")),
            eq(Optional.empty())))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new UpdateIndividualRequest("Jane Rochester", null);
    testKit.run(
        new IndividualCommand.PatchIndividual(
            tenantId, individualId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Success.class);
  }

  @Test
  @DisplayName("onPatchIndividual returns BadRequest on blank name")
  void onPatchIndividual_blankName_returnsBadRequest() {
    var individualDao = mock(IndividualDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new UpdateIndividualRequest("   ", null);
    testKit.run(
        new IndividualCommand.PatchIndividual(
            tenantId, individualId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchIndividual returns NotFound when entity missing")
  void onPatchIndividual_notFound() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.patch(any(), any(), any(), any())).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new UpdateIndividualRequest("Jane Rochester", null);
    testKit.run(
        new IndividualCommand.PatchIndividual(
            tenantId, individualId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchIndividual returns Failure on DAO exception")
  void onPatchIndividual_exception() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.patch(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Patch failed"));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new UpdateIndividualRequest("Jane Rochester", null);
    testKit.run(
        new IndividualCommand.PatchIndividual(
            tenantId, individualId, request, Optional.empty(), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onDeleteIndividual returns Deleted when softDelete returns true")
  void onDeleteIndividual_success() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.softDelete(tenantId, individualId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.DeleteIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Deleted.class);
  }

  @Test
  @DisplayName("onDeleteIndividual returns NotFound when softDelete returns false")
  void onDeleteIndividual_notFound() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.softDelete(tenantId, individualId)).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.DeleteIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onDeleteIndividual returns Failure on DAO exception")
  void onDeleteIndividual_exception() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.softDelete(any(), any())).thenThrow(new RuntimeException("Delete failed"));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.DeleteIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onPatchIndividual with updateMask omitting field ignores that field")
  void onPatchIndividual_maskOmitsField() {
    var individualDao = mock(IndividualDAO.class);
    var updated =
        new Individual(individualId, "Jane Eyre", "New summary", now, now, (Instant) null);
    when(individualDao.patch(
            eq(tenantId), eq(individualId), eq(Optional.empty()), eq(Optional.of("New summary"))))
        .thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    var request = new UpdateIndividualRequest("Jane Rochester", "New summary");
    testKit.run(
        new IndividualCommand.PatchIndividual(
            tenantId, individualId, request, Optional.of("summary"), inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Success.class);
  }

  @Test
  @DisplayName("handleError with null message uses default message")
  void onGetIndividual_nullMessageException() {
    var individualDao = mock(IndividualDAO.class);
    when(individualDao.findById(any(), any())).thenThrow(new RuntimeException((String) null));

    var testKit = BehaviorTestKit.create(createBehavior(individualDao));
    TestInbox<IndividualActorResponse> inbox = TestInbox.create();

    testKit.run(new IndividualCommand.GetIndividual(tenantId, individualId, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(IndividualActorResponse.Failure.class);
    assertThat(((IndividualActorResponse.Failure) response).message()).contains("Error executing");
  }
}
