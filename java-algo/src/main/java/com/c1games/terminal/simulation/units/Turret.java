package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;

import java.util.function.Function;

public class Turret extends StructureUnit {
  private static final double startHealth = 60;
  private static final double range = 2.5; //max euclidean distance
  private static final double walkerDamage = 6;
  private static final double structureDamage = 0;

  Turret(Unit turret, Coords location) {
    super(turret.owner == PlayerId.Player2, turret.id, turret.unitInformation.startHealth.orElse(Turret.startHealth), turret.unitInformation.attackRange.orElse(Turret.range), turret.unitInformation.attackDamageWalker.orElse(Turret.walkerDamage), turret.unitInformation.attackDamageTower.orElse(Turret.structureDamage), location, turret.health, turret.upgraded);
  }

  @Override
  public boolean isInteractable(SimUnit simUnit) {
    return simUnit instanceof MobileUnit && simUnit.isEnemy() != this.isEnemy();
  }
}
