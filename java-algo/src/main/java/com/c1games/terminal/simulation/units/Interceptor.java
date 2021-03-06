package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.simulation.pathfinding.Direction;

import java.util.function.Function;

public class Interceptor extends MobileUnit {
  private static final double startHealth = 40;
  private static final double range = 4.5; //max euclidean distance
  private static final double walkerDamage = 20;
  private static final double structureDamage = 0;
  private static final double speed = 4; //num frames to move

  Interceptor(Unit interceptor, Coords location) {
    super(interceptor.owner == PlayerId.Player2, interceptor.id, interceptor.unitInformation.startHealth.orElse(Interceptor.startHealth), interceptor.unitInformation.attackRange.orElse(Interceptor.range), interceptor.unitInformation.attackDamageWalker.orElse(Interceptor.walkerDamage), interceptor.unitInformation.attackDamageTower.orElse(Interceptor.structureDamage), interceptor.unitInformation.speed.orElse(Interceptor.speed), location, interceptor.health, Direction.SPAWNED);
  }

  @Override
  public boolean isInteractable(SimUnit simUnit) {
    return simUnit instanceof MobileUnit && simUnit.isEnemy() != this.isEnemy();
  }


}
