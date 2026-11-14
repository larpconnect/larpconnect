package com.larpconnect.njall.data.domain.studios;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentDomainTest {

  @Test
  @DisplayName("Agent canonical constructor retains all fields and computes isDeleted")
  void canonicalConstructor_retainsFields() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var inboxId = UUID.randomUUID();
    var outboxId = UUID.randomUUID();
    var now = Instant.now();

    var agent =
        new Agent(
            id,
            generator,
            "valiant-bot",
            Optional.of("Automated herald"),
            inboxId,
            outboxId,
            ImmutableMap.of("sharedInbox", "https://example.com/inbox"),
            now,
            now,
            Optional.empty());

    assertThat(agent.id()).isEqualTo(id);
    assertThat(agent.generator()).isEqualTo(generator);
    assertThat(agent.preferredUsername()).isEqualTo("valiant-bot");
    assertThat(agent.summary()).contains("Automated herald");
    assertThat(agent.inboxId()).isEqualTo(inboxId);
    assertThat(agent.outboxId()).isEqualTo(outboxId);
    assertThat(agent.endpoints()).containsEntry("sharedInbox", "https://example.com/inbox");
    assertThat(agent.createdOn()).isEqualTo(now);
    assertThat(agent.updatedOn()).isEqualTo(now);
    assertThat(agent.deletedOn()).isEmpty();
    assertThat(agent.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Agent nullable constructor wraps nulls into empty Optionals")
  void nullableConstructor_wrapsNulls() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var inboxId = UUID.randomUUID();
    var outboxId = UUID.randomUUID();
    var now = Instant.now();

    var agent =
        new Agent(
            id,
            generator,
            "valiant-bot",
            null,
            inboxId,
            outboxId,
            Map.of("inbox", "https://example.com/u/valiant/inbox"),
            now,
            now,
            null);

    assertThat(agent.generator()).isEqualTo(generator);
    assertThat(agent.summary()).isEmpty();
    assertThat(agent.endpoints()).containsEntry("inbox", "https://example.com/u/valiant/inbox");
    assertThat(agent.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Agent isDeleted returns true when deletedOn is present")
  void isDeleted_returnsTrueWhenDeletedOnPresent() {
    var now = Instant.now();
    var agent =
        new Agent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "retired-bot",
            "Retired",
            UUID.randomUUID(),
            UUID.randomUUID(),
            Map.of(),
            now,
            now,
            now);

    assertThat(agent.isDeleted()).isTrue();
    assertThat(agent.deletedOn()).contains(now);
  }
}
