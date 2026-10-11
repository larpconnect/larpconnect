package com.larpconnect.njall.api.studios.individuals;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
import com.larpconnect.njall.data.domain.Individual;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted individual operations. */
final class IndividualActor extends AbstractBehavior<IndividualCommand> {

  private final Logger logger = LoggerFactory.getLogger(IndividualActor.class);
  private final IndividualDAO individualDao;

  IndividualActor(ActorContext<IndividualCommand> context, IndividualDAO individualDao) {
    super(context);
    this.individualDao = individualDao;
  }

  @Override
  public Receive<IndividualCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(IndividualCommand.CreateIndividual.class, this::onCreateIndividual)
        .onMessage(IndividualCommand.GetIndividual.class, this::onGetIndividual)
        .onMessage(IndividualCommand.PatchIndividual.class, this::onPatchIndividual)
        .onMessage(IndividualCommand.DeleteIndividual.class, this::onDeleteIndividual)
        .build();
  }

  private Behavior<IndividualCommand> onCreateIndividual(IndividualCommand.CreateIndividual cmd) {
    try {
      var validationError = IndividualValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(IndividualActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executeCreate(cmd.tenantId(), cmd.request(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create individual", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId, CreateIndividualRequest request, ActorRef<IndividualActorResponse> replyTo) {
    var domain = individualDao.create(tenantId, request.name(), request.summary());
    replyTo.tell(IndividualActorResponse.success(toIndividualResponse(domain)));
  }

  private Behavior<IndividualCommand> onGetIndividual(IndividualCommand.GetIndividual cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.individualId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get individual", e);
    }
    return this;
  }

  private void executeGet(
      UUID tenantId, UUID individualId, ActorRef<IndividualActorResponse> replyTo) {
    individualDao
        .findById(tenantId, individualId)
        .ifPresentOrElse(
            individual ->
                replyTo.tell(IndividualActorResponse.success(toIndividualResponse(individual))),
            () ->
                replyTo.tell(
                    IndividualActorResponse.notFound("Individual not found: " + individualId)));
  }

  private Behavior<IndividualCommand> onPatchIndividual(IndividualCommand.PatchIndividual cmd) {
    try {
      var validationError = IndividualValidation.validatePatch(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(IndividualActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executePatch(
          cmd.tenantId(), cmd.individualId(), cmd.request(), cmd.updateMask(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch individual", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      UUID individualId,
      UpdateIndividualRequest request,
      Optional<String> updateMask,
      ActorRef<IndividualActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var name = selectField(maskFields, "name", request.name());
    var summary = selectField(maskFields, "summary", request.summary());

    individualDao
        .patch(tenantId, individualId, name, summary)
        .ifPresentOrElse(
            updated -> replyTo.tell(IndividualActorResponse.success(toIndividualResponse(updated))),
            () ->
                replyTo.tell(
                    IndividualActorResponse.notFound("Individual not found: " + individualId)));
  }

  private Behavior<IndividualCommand> onDeleteIndividual(IndividualCommand.DeleteIndividual cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.individualId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete individual", e);
    }
    return this;
  }

  private void executeDelete(
      UUID tenantId, UUID individualId, ActorRef<IndividualActorResponse> replyTo) {
    var deleted = individualDao.softDelete(tenantId, individualId);
    if (deleted) {
      replyTo.tell(IndividualActorResponse.deleted());
    } else {
      replyTo.tell(IndividualActorResponse.notFound("Individual not found: " + individualId));
    }
  }

  private IndividualResponse toIndividualResponse(Individual individual) {
    return new IndividualResponse(
        individual.id(),
        individual.name(),
        individual.summary(),
        individual.createdOn(),
        individual.updatedOn());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private <T> Optional<T> selectField(
      Optional<Set<String>> maskFields, String field, Optional<T> requestValue) {
    return maskFields
        .map(fields -> fields.contains(field) ? requestValue : Optional.<T>empty())
        .orElse(requestValue);
  }

  private void handleError(
      ActorRef<IndividualActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in IndividualActor", operation, error);
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(IndividualActorResponse.failure(reason));
  }
}
