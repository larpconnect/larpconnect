package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class EntityTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    Entity entity = Mockito.mock(Entity.class, Mockito.CALLS_REAL_METHODS);

    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    ExternalResource externalResourceVal = Mockito.mock(ExternalResource.class);
    entity.setExternalResource(externalResourceVal);
    assertThat(entity.getExternalResource()).isEqualTo(externalResourceVal);
    OffsetDateTime createdOnVal = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    entity.setCreatedOn(createdOnVal);
    assertThat(entity.getCreatedOn()).isEqualTo(createdOnVal);
    OffsetDateTime updatedOnVal = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    entity.setUpdatedOn(updatedOnVal);
    assertThat(entity.getUpdatedOn()).isEqualTo(updatedOnVal);
    OffsetDateTime deletedOnVal = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    entity.setDeletedOn(deletedOnVal);
    assertThat(entity.getDeletedOn()).isEqualTo(deletedOnVal);
  }
}
