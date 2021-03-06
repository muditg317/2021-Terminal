package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.Unit;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class SupportTower extends StructureUnit {
  private static final double startHealth = 1;
  private static final double range = 3.5; //max euclidean distance
  private static final double walkerDamage = 0;
  private static final double structureDamage = 0;

  private final double shieldAmount;
  private final Set<SimUnit> shielded;

  SupportTower(Unit supportTower, Coords location) {
    super(supportTower.owner == PlayerId.Player2, supportTower.id, supportTower.unitInformation.startHealth.orElse(SupportTower.startHealth), supportTower.unitInformation.attackRange.orElse(SupportTower.range), supportTower.unitInformation.attackDamageWalker.orElse(SupportTower.walkerDamage), supportTower.unitInformation.attackDamageTower.orElse(SupportTower.structureDamage), location, supportTower.health, supportTower.upgraded);
    shielded = new HashSet<>();
    this.shieldAmount = supportTower.unitInformation.shieldPerUnit.orElse(supportTower.upgraded ? 5 : 3) + (supportTower.upgraded ? (location.y * supportTower.unitInformation.shieldBonusPerY.orElse(0.3)) : 0);
  }

  @Override
  public boolean isInteractable(SimUnit simUnit) {
    return simUnit instanceof MobileUnit && simUnit.isEnemy() == super.isEnemy() && !shielded.contains(simUnit);
  }

  public void shield(MobileUnit unit) {
    if (this.shielded.add(unit)) {
      unit.applyShielding(shieldAmount);
    }
  }

  public double getShieldAmount() {
    return shieldAmount;
  }
}
