package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;

import java.util.HashSet;
import java.util.Set;

public abstract class MobileUnit extends SimUnit {
  public double speed; // num frames to move

  public Direction prevDirection;
  public Set<StructureUnit> previouslyInRange;
  public int framesSinceMoved;
  public int timesMoved;

  public MobileUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, double speed, Coords location, double health, Direction prevDirection) {
    super(isEnemy, id, startHealth, range, walkerDamage, structureDamage, location, health);
    this.speed = speed;
    this.prevDirection = prevDirection;
    this.previouslyInRange = new HashSet<>();
    this.framesSinceMoved = 0;
    this.timesMoved = 0;
  }

  @Override
  public boolean testUnitInteractable(SimUnit other) {
    // TODO: optimization for when this boi hasn't moved
    return super.testUnitInteractable(other);
  }
}
