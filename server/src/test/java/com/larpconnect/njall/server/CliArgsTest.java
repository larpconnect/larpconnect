package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CliArgsTest {

  @Test
  @DisplayName("parse returns migrate false when args is null")
  void parse_nullArgs_returnsFalse() {
    var args = CliArgs.parse(null);
    assertThat(args.migrate()).isFalse();
  }

  @Test
  @DisplayName("parse returns migrate false when args is empty")
  void parse_emptyArgs_returnsFalse() {
    var args = CliArgs.parse(new String[0]);
    assertThat(args.migrate()).isFalse();
  }

  @Test
  @DisplayName("parse returns migrate false when --migrate is not present")
  void parse_otherFlags_returnsFalse() {
    var args = CliArgs.parse(new String[] {"--debug", "--port", "9000"});
    assertThat(args.migrate()).isFalse();
  }

  @Test
  @DisplayName("parse returns migrate true when --migrate is present")
  void parse_migrateFlagPresent_returnsTrue() {
    var args = CliArgs.parse(new String[] {"--migrate"});
    assertThat(args.migrate()).isTrue();
  }

  @Test
  @DisplayName("parse returns migrate true when --migrate is mixed with other arguments")
  void parse_mixedArgsWithMigrate_returnsTrue() {
    var args = CliArgs.parse(new String[] {"--foo", "--migrate", "--bar"});
    assertThat(args.migrate()).isTrue();
  }
}
