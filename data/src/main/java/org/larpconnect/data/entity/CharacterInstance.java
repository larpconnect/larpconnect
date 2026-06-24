package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** CharacterInstance entity scoped to a tenant schema. */
@Entity
@Table(name = "character_instances")
public final class CharacterInstance extends TenantEntity {
  @ManyToOne
  @JoinColumn(name = "character_id", nullable = false)
  private LarpCharacter character;

  @ManyToOne
  @JoinColumn(name = "player_id", nullable = false)
  private Individual player;

  @ManyToOne
  @JoinColumn(name = "game_id")
  private Game game;

  @Column(name = "individual_name", nullable = false)
  private String individualName;

  public CharacterInstance() {}

  public CharacterInstance(
      UUID id, LarpCharacter character, Individual player, Game game, String individualName) {
    super(id);
    this.character = character;
    this.player = player;
    this.game = game;
    this.individualName = individualName;
  }

  public LarpCharacter getCharacter() {
    return character;
  }

  public void setCharacter(LarpCharacter character) {
    this.character = character;
  }

  public Individual getPlayer() {
    return player;
  }

  public void setPlayer(Individual player) {
    this.player = player;
  }

  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
  }

  public String getIndividualName() {
    return individualName;
  }

  public void setIndividualName(String individualName) {
    this.individualName = individualName;
  }
}
