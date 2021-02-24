package com.c1games.terminal.myalgo;

public class AttackBreakdown {
  /**
   * Describes where this attack should originate from
   * TODO: consider replacing with a Coords object
   */
  String location;
  /**
   * The units needed for this attack
   */
  UnitCounts units;

  public AttackBreakdown(String location, UnitCounts units) {
    this.location = location;
    this.units = units;
  }
}
