package com.larpconnect.njall.data.dao.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CampaignEntityTest {

  @Test
  @DisplayName("CampaignEntity no-arg constructor initializes null defaults")
  void noArgConstructor_defaultsToNull() {
    var entity = new CampaignEntity();

    assertThat(entity.getTenantId()).isNull();
    assertThat(entity.getId()).isNull();
    assertThat(entity.getSystemId()).isNull();
    assertThat(entity.getTitle()).isNull();
  }

  @Test
  @DisplayName("CampaignEntity constructor and setters update systemId and title")
  void constructorAndSetters_updateMutableFields() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var initialSystemId = UUID.randomUUID();
    var updatedSystemId = UUID.randomUUID();

    var entity = new CampaignEntity(tenantId, id, initialSystemId, "Seattle By Night");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getSystemId()).isEqualTo(initialSystemId);
    assertThat(entity.getTitle()).isEqualTo("Seattle By Night");

    entity.setSystemId(updatedSystemId);
    entity.setTitle("Chicago By Night");
    assertThat(entity.getSystemId()).isEqualTo(updatedSystemId);
    assertThat(entity.getTitle()).isEqualTo("Chicago By Night");

    entity.setSystemId(null);
    assertThat(entity.getSystemId()).isNull();
  }

  @Test
  @DisplayName("CampaignEntity constructor supports null systemId for systemless campaigns")
  void constructor_nullSystemId_supported() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var entity = new CampaignEntity(tenantId, id, null, "Anthology");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getSystemId()).isNull();
    assertThat(entity.getTitle()).isEqualTo("Anthology");
  }
}
