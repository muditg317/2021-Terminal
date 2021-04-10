package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;

import java.util.function.Function;

public class Wall extends StructureUnit {
  private static final double startHealth = 60;
  private static final double range = 0; //max euclidean distance
  private static final double walkerDamage = 0;
  private static final double structureDamage = 0;

  Wall(Unit wall, Coords location) {
    super(wall.owner == PlayerId.Player2, wall.id, wall.unitInformation.startHealth.orElse(Wall.startHealth), wall.unitInformation.attackRange.orElse(Wall.range), wall.unitInformation.attackDamageWalker.orElse(Wall.walkerDamage), wall.unitInformation.attackDamageTower.orElse(Wall.structureDamage), location, wall.health, wall.upgraded);
  }

  @Override
  public boolean isInteractable(SimUnit simUnit) {
    return false;
  }
}
