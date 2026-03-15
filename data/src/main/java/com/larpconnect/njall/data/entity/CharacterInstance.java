package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "character_instances")
public class CharacterInstance extends com.larpconnect.njall.data.entity.Entity {
  public CharacterInstance() {}

  @ManyToOne
  @JoinColumn(name = "character_id")
  private LarpCharacter character;

  @ManyToOne
  @JoinColumn(name = "player_id")
  private Individual player;

  @ManyToOne
  @JoinColumn(name = "game_id")
  private Game game;

  @Column(name = "individual_name")
  private String individualName;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public LarpCharacter getCharacter() {
    return character;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setCharacter(LarpCharacter character) {
    this.character = character;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Individual getPlayer() {
    return player;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setPlayer(Individual player) {
    this.player = player;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Game getGame() {
    return game;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setGame(Game game) {
    this.game = game;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getIndividualName() {
    return individualName;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setIndividualName(String individualName) {
    this.individualName = individualName;
  }
}
