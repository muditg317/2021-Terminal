package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.simulation.pathfinding.Direction;

import java.util.function.Function;

public class Demolisher extends MobileUnit {
  private static final double startHealth = 5;
  private static final double range = 4.5; //max euclidean distance
  private static final double walkerDamage = 8;
  private static final double structureDamage = 8;
  private static final double speed = 2; //num frames to move


  Demolisher(Unit demolisher, Coords location) {
    super(demolisher.owner == PlayerId.Player2, demolisher.id, demolisher.unitInformation.startHealth.orElse(Demolisher.startHealth), demolisher.unitInformation.attackRange.orElse(Demolisher.range), demolisher.unitInformation.attackDamageWalker.orElse(Demolisher.walkerDamage), demolisher.unitInformation.attackDamageTower.orElse(Demolisher.structureDamage), demolisher.unitInformation.speed.orElse(Demolisher.speed), location, demolisher.health, Direction.SPAWNED);
  }

  @Override
  public boolean isInteractable(SimUnit simUnit) {
    return simUnit.isEnemy() != this.isEnemy();
  }

}
