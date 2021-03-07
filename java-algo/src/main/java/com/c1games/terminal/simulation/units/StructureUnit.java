package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;

public abstract class StructureUnit extends SimUnit {
  public boolean upgraded;

  protected StructureUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, Coords location, double health, boolean upgraded) {
    super(isEnemy, id, startHealth, range, walkerDamage, structureDamage, location, health);
    this.upgraded = upgraded;
  }

  public static char toChar(SimUnit unit) {
    char c = unit instanceof Wall ? 'o' : (unit instanceof Turret ? 'x' : 's');
    return ((StructureUnit) unit).upgraded ? Character.toUpperCase(c) : c;
  }
}
