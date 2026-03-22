package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ExternalResourceTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new ExternalResource();

    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    String externalUriVal = "test";
    entity.setExternalUri(externalUriVal);
    assertThat(entity.getExternalUri()).isEqualTo(externalUriVal);
    String dataVal = "test";
    entity.setData(dataVal);
    assertThat(entity.getData()).isEqualTo(dataVal);
    OffsetDateTime lastRefreshVal = OffsetDateTime.now(java.time.ZoneId.systemDefault());
    entity.setLastRefresh(lastRefreshVal);
    assertThat(entity.getLastRefresh()).isEqualTo(lastRefreshVal);
    String hostnameVal = "test";
    entity.setHostname(hostnameVal);
    assertThat(entity.getHostname()).isEqualTo(hostnameVal);
  }
}
