package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CharacterInstanceTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    CharacterInstance entity = new CharacterInstance();

    Character characterVal = mock(Character.class);
    entity.setCharacter(characterVal);
    assertThat(entity.getCharacter()).isEqualTo(characterVal);
    Individual playerVal = mock(Individual.class);
    entity.setPlayer(playerVal);
    assertThat(entity.getPlayer()).isEqualTo(playerVal);
    Game gameVal = mock(Game.class);
    entity.setGame(gameVal);
    assertThat(entity.getGame()).isEqualTo(gameVal);
    String individualNameVal = "test";
    entity.setIndividualName(individualNameVal);
    assertThat(entity.getIndividualName()).isEqualTo(individualNameVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
