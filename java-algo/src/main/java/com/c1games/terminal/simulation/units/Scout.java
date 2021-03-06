package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.simulation.pathfinding.Direction;

import java.util.function.Function;

public class Scout extends MobileUnit {
  private static final double startHealth = 15;
  private static final double range = 3.5; //max euclidean distance
  private static final double walkerDamage = 2;
  private static final double structureDamage = 2;
  private static final double speed = 1; //num frames to move

  Scout(Unit scout, Coords location) {
    super(scout.owner == PlayerId.Player2, scout.id, scout.unitInformation.startHealth.orElse(Scout.startHealth), scout.unitInformation.attackRange.orElse(Scout.range), scout.unitInformation.attackDamageWalker.orElse(Scout.walkerDamage), scout.unitInformation.attackDamageTower.orElse(Scout.structureDamage), scout.unitInformation.speed.orElse(Scout.speed), location, scout.health, Direction.SPAWNED);
  }

  @Override
  public boolean isInteractable(SimUnit simUnit) {
    return simUnit.isEnemy() != this.isEnemy();
  }


}
