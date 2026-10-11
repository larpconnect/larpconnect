package com.larpconnect.njall.data.dao.studios.individuals;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class IndividualEntityTest {

  @Test
  @DisplayName("IndividualEntity getters and setters operate correctly")
  void individualEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var defaultIndividual = new IndividualEntity();
    assertThat(defaultIndividual.getId()).isNull();

    var fullIndividual = new IndividualEntity(tenantId, id, "Jane Eyre");
    fullIndividual.setName("Jane Rochester");

    assertThat(fullIndividual.getTenantId()).isEqualTo(tenantId);
    assertThat(fullIndividual.getId()).isEqualTo(id);
    assertThat(fullIndividual.getName()).isEqualTo("Jane Rochester");
  }

  @Test
  @DisplayName("IndividualEntity constructor initializes all fields")
  void individualEntity_constructor() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var individual = new IndividualEntity(tenantId, id, "Edward Rochester");

    assertThat(individual.getTenantId()).isEqualTo(tenantId);
    assertThat(individual.getId()).isEqualTo(id);
    assertThat(individual.getName()).isEqualTo("Edward Rochester");
  }
}
