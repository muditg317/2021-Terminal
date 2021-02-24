package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;

public abstract class StructureUnit extends SimUnit {
  public StructureUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, int speed, Coords location, double health, double shield, Direction prevDirection) {
    super(isEnemy, id, startHealth, range, walkerDamage, structureDamage, location, health);
  }
}
