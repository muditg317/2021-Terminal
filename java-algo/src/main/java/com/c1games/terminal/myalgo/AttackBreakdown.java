package com.c1games.terminal.myalgo;

public class AttackBreakdown {
  /**
   * Describes where this attack should originate from
   * TODO: consider replacing with a Coords object
   */
  Side side;
  /**
   * The units needed for this attack
   */
  UnitCounts units;

  public AttackBreakdown(Side side, UnitCounts units) {
    this.side = side;
    this.units = units;
  }
}
