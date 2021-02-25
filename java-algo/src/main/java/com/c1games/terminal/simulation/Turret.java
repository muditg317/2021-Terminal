package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;

import java.util.function.Function;

public class Turret extends StructureUnit {
  private static final double startHealth = 60;
  private static final double range = 2.5; //max euclidean distance
  private static final double walkerDamage = 6;
  private static final double structureDamage = 0;
  private final Function<SimUnit, Boolean> interactiblityFilter;

  public Turret(Unit scout, Coords location) {
    super(scout.owner == PlayerId.Player2, scout.id, scout.unitInformation.startHealth.orElse(Turret.startHealth), scout.unitInformation.attackRange.orElse(Turret.range), scout.unitInformation.attackDamageWalker.orElse(Turret.walkerDamage), scout.unitInformation.attackDamageTower.orElse(Turret.structureDamage), location, scout.health);
    interactiblityFilter = simUnit -> simUnit instanceof MobileUnit && simUnit.isEnemy != this.isEnemy;
  }

  @Override
  public Function<SimUnit, Boolean> getInteractabilityFilter() {
    return interactiblityFilter;
  }
}
