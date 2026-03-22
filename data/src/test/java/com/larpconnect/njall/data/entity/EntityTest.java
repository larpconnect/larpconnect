package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class EntityTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    Entity entity = mock(Entity.class, CALLS_REAL_METHODS);

    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    ExternalResource externalResourceVal = mock(ExternalResource.class);
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
