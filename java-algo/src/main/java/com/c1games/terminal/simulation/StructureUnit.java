package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;

public abstract class StructureUnit extends SimUnit {
  public StructureUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, Coords location, double health) {
    super(isEnemy, id, startHealth, range, walkerDamage, structureDamage, location, health);
  }
}
