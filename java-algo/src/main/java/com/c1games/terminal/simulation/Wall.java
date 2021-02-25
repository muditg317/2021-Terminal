package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;

import java.util.function.Function;

public class Wall extends StructureUnit {
  private static final double startHealth = 60;
  private static final double range = 0; //max euclidean distance
  private static final double walkerDamage = 0;
  private static final double structureDamage = 0;
  private static final Function<SimUnit, Boolean> interactiblityFilter = simUnit -> false;

  public Wall(Unit scout, Coords location) {
    super(scout.owner == PlayerId.Player2, scout.id, scout.unitInformation.startHealth.orElse(Wall.startHealth), scout.unitInformation.attackRange.orElse(Wall.range), scout.unitInformation.attackDamageWalker.orElse(Wall.walkerDamage), scout.unitInformation.attackDamageTower.orElse(Wall.structureDamage), location, scout.health);
  }

  @Override
  public Function<SimUnit, Boolean> getInteractabilityFilter() {
    return interactiblityFilter;
  }
}
